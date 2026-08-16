# Pannkoogihommiku planeerija

JavaFX-is tehtud töölauarakendus ürituse alaplaani ja elektrivajaduse planeerimiseks. Rakenduses saab paigutada kaardile telke, elektrikappe ja teisi objekte, ühendada tarbijaid konkreetsete väljunditega, koostada kaablite trajektoore ning salvestada ja eksportida valminud plaani.

Projekt sai alguse varasema ühefaililise Pythoni rakenduse objektorienteeritud ümbertegemisest. Pikem eesmärk on kasutada siin kujunevat arhitektuuri ja kasutuskogemust bakalaureusetööna arendatava suurema ürituste planeerimise süsteemi alusena.

Põhjalik ülevaade algsetest eesmärkidest, senisest arendusest, praegusest seisust ja järgmistest töödest asub failis [docs/PROJEKTI_ULEVAADE.md](docs/PROJEKTI_ULEVAADE.md).

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

## Plaanifailid

Uued `.pplan` failid salvestatakse versioon 2 ZIP-paketina. Pakett sisaldab plaani andmeid ja kasutaja valitud PNG- või JPEG-kaarti, mistõttu piisab plaani teise arvutisse viimiseks ühest `.pplan` failist. Projektiga kaasas olevatele vaikekaartidele säilitatakse paketis viide ning neid ei dubleerita.

Rakendus avab edasi ka vanad versioonita ja versioon 1 properties-vormingus `.pplan` failid. Vana fail teisendatakse versioon 2 paketiks alles siis, kui kasutaja selle järgmine kord salvestab.

## Tavakasutajale jagamine

Windowsi paigalduspaketti ei ole veel loodud. Enne rakenduse jagamist tehnilise ettevalmistuseta kasutajatele tuleb lisada `jpackage`-põhine pakkimine, mis sisaldab rakenduse käitamiseks vajalikku Java runtime'i.
