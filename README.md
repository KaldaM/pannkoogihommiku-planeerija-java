# Plaanisepp

Plaanisepp on JavaFX-is tehtud töölauarakendus sündmusala ja elektrivajaduse planeerimiseks. Rakenduses saab paigutada kaardile telke, elektrikappe ja teisi objekte, ühendada tarbijaid konkreetsete väljunditega, koostada kaablite trajektoore ning salvestada ja eksportida valminud plaani.

Projekt sai alguse varasema ühefaililise Pythoni rakenduse objektorienteeritud ümbertegemisest. Pikem eesmärk on kasutada siin kujunevat arhitektuuri ja kasutuskogemust bakalaureusetööna arendatava suurema ürituste planeerimise süsteemi alusena.

Põhjalik ülevaade algsetest eesmärkidest, senisest arendusest ja praegusest seisust asub failis [docs/PROJEKTI_ULEVAADE.md](docs/PROJEKTI_ULEVAADE.md). Kavandatud kasutajaliidese muudatused ja nimevahetuse otsused on koondatud faili [docs/ARENDUSPLAAN.md](docs/ARENDUSPLAAN.md).

## Moodulid

- `planner-core` - plaani domeenimudel, vooluarvutused ja salvestamise loogika.
- `planner-gui` - JavaFX-i kasutajaliides, kaardivaade ja ekspordid.

## Tehnoloogiad

- Java 25 LTS
- JavaFX 26.0.2
- Gradle Wrapper
- Apache PDFBox
- JUnit 5

## Käivitamine arenduses

Windowsis:

```powershell
.\gradlew.bat :planner-gui:run
```

Projekt avaneb IntelliJ IDEA-s Gradle'i projektina. Rakendus käivitub maksimeeritud aknas ja uus plaan on tühi.

## Kontrollimine

```powershell
.\gradlew.bat test
```

Automaattestid katavad muu hulgas domeenimudelit, vooluarvutusi, geomeetriat, eksporti ning `.pplan` failide tagasiühilduvust ja paketivormingut. JavaFX-i kasutajaliidese sündmuste testikate vajab veel laiendamist.

GitHub Actions käivitab iga push'i ja pull request'i korral Java 25-ga automaatselt puhta `test`-töövoo.

## Plaanifailid

Uued `.pplan` failid salvestatakse versioon 3 ZIP-paketina. Pakett sisaldab plaani andmeid ja kasutaja valitud PNG- või JPEG-kaarti, mistõttu piisab plaani teise arvutisse viimiseks ühest `.pplan` failist. Versioon 3 säilitab ka objektide mitu vooluühendust ja seadmete ühendusevalikud. Projektiga kaasas olevatele vaikekaartidele säilitatakse paketis viide ning neid ei dubleerita.

Rakendus avab edasi vanad versioonita ja versioon 1 properties-vormingus ning versioon 2 ZIP-paketina salvestatud `.pplan` failid. Vana fail teisendatakse versioon 3 paketiks alles siis, kui kasutaja selle järgmine kord salvestab.

## Tavakasutajale jagamine

### Windows

Windowsis saab luua iseseisva rakendusepildi, mis sisaldab vajalikku Java runtime'i, JavaFX-i ja kõiki muid käitusaegseid sõltuvusi:

```powershell
.\gradlew.bat :planner-gui:packageWindowsAppImage
```

Valmis rakendus asub kaustas `planner-gui/build/jpackage-windows/Plaanisepp`.

Windowsi EXE-paigaldaja loomiseks peavad lisaks Java 25-le olema paigaldatud [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0) ja ametlik [WiX Toolset 4](https://docs.firegiant.com/wix/using-wix/). Seejärel tuleb käivitada:

```powershell
.\gradlew.bat :planner-gui:packageWindowsInstaller
```

Paigaldaja luuakse kausta `planner-gui/build/jpackage-windows-installer`. Paigaldatud rakendus lisatakse Start-menüüsse ning `.pplan` failitüüp seotakse rakendusega, nii et plaani saab avada topeltklõpsuga. Kasutaja arvutisse ei pea olema eraldi Javat, Gradle'it ega repository't paigaldatud.

Kohalik arendusbuild ei ole digitaalselt allkirjastatud. Avalikult levitatav paigaldaja tuleb enne väljalaset usaldusväärse koodisigneerimise sertifikaadiga allkirjastada.

### Linux

Linuxis saab luua iseseisva rakendusekausta, mis sisaldab vajalikku Java runtime'i ja JavaFX-i:

```bash
./gradlew :planner-gui:packageLinuxAppImage
```

Valmis rakendus asub kaustas `planner-gui/build/jpackage/plaanisepp`. Selle käivitaja on `bin/plaanisepp` ning kasutaja arvutisse ei pea olema eraldi Javat ega Gradle'it paigaldatud.

Fedora RPM-paigalduspaketi loomiseks:

```bash
./gradlew :planner-gui:packageLinuxRpm
```

Valmis pakett asub kaustas `planner-gui/build/jpackage-rpm`. Selle saab paigaldada ja hiljem eemaldada järgmiselt:

```bash
sudo dnf install ./planner-gui/build/jpackage-rpm/plaanisepp-0.1.1-4.x86_64.rpm
sudo dnf remove plaanisepp
```

RPM paigaldab rakenduse `/opt/plaanisepp` alla, lisab rakenduste menüüsse kirje „Plaanisepp” ning seob `.pplan` failid rakendusega. Uus RPM asendab varasema tehnilise nimega `pannkoogihommiku-planeerija` paketi. Paigaldamise järel saab plaani avada failihalduris topeltklõpsuga. Rakenduse aknas, menüüs ja plaanifailidel kasutatakse projekti enda ikooni.

`jpackage` paketid tuleb koostada ning kontrollida sellel operatsioonisüsteemil, millele need on mõeldud.
