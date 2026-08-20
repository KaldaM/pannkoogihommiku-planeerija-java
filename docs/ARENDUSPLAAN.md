# Rakenduse edasine arendusplaan

- Koostatud: 20. august 2026
- Lähtepunkt: commit `e53fb3a` (`Extract marker icon factory from application`)

## Eesmärk

Rakendus ei ole enam ainult pannkoogihommiku töövahend. Edasine arendus peab toetama eri organisatsioonide ja eri tüüpi ürituste alaplaanide koostamist, säilitades olemasolevate `.pplan` failide avatavuse ning elektri- ja kaabliplaneerimise tugevused.

## Soovituslik tööjärjekord

1. lisada enne sügist alajaotuskilbid ning seadmete vaiketoide ja seadmepõhised toitevalikud;
2. kujundada elektri kokkuvõte uue mudeli põhjal interaktiivseks ning lisada väljundite koormusribad;
3. lisada objektide kiirotsing kahekordse Shift-klahviga;
4. asendada suumi `+` ja `-` nupud liuguriga ning lisada `Alt + hiirerull`;
5. jätkata `PlaaniseppApp` refaktoreerimist väikeste, funktsioonidega seotud sammudena.

Rakenduse nimeks valiti 20. augustil 2026 **Plaanisepp**. Nimi kirjeldab plaanide meistrit ja seostub ka 1927. aastal talletatud Lõuna-Eesti nimekujuga „Plaani sepp”.

## 1. Rakenduse nimi

### Nimeotsus

**Plaanisepp** sobib pannkoogihommikust laiemale sündmuste planeerimisele ning katab nii kaardi, objektide, elektri kui ka kaablite tervikuks vormimise. Nimi kasutab ainult ASCII-tähti, on hõlpsasti käänatav ja sobib rakenduse, paigaldaja ning võimaliku tulevase veebiteenuse nimeks.

Kasutajale nähtav nimi, pakendinimed ja Java paketid muudetakse Plaanisepa järgi. Tagasiühilduvuse tõttu säilivad eraldi ajalooliste tehniliste identifikaatoritena `.pplan` sisemised vormingutunnused, MIME-tüüp, eelistuste asukoht `/ee/matteus/pannukas/gui` ja Windowsi uuenduse UUID.

### Ümbernimetamise ulatus

Nime muutmisel tuleb eraldi kontrollida:

- rakenduse akna pealkiri ja kasutajale kuvatavad tekstid;
- README ja projekti dokumentatsioon;
- Windowsi rakendusepilt ning EXE-paigaldaja;
- Linuxi rakendusepilt, RPM, töölauakirje ja paketinimi;
- rakenduse ning `.pplan` failitüübi ikoonid;
- Java `Preferences` võtmete säilimine;
- paigaldatud vana versiooni uuendamine ja eemaldamine.

`.pplan` laiend on piisavalt üldine ja selle muutmiseks praegu vajadust ei ole. Java paketid ja repository nimi ei pea kasutajale nähtava nimega samas commit'is muutuma.

## 2. Alajaotuskilbid ja seadmepõhine voolujaotus

See on sügis-eelse väljalaske kriitiline funktsioon ning tuleb teostada enne elektri külgpaneeli visuaalset ümberkujundamist.

### Kasutusjuht

Ühe telgi, ala või joone seadmed ei pruugi kõik saada voolu samast kohast. Objektil on vaiketoide, mida kasutavad automaatselt kõik seadmed, millele ei ole määratud erandit. Üksikule seadmele saab valida teise elektrikapi või alajaotuskilbi väljundi.

Näide:

```text
Kohvitelk
├── vaiketoide: alajaotuskilp A / väljund 1
├── kohvimasin       1800 W   kasutab vaiketoidet
├── veekeetja        2000 W   kasutab vaiketoidet
└── külmik            500 W   püsivoolukilp B / väljund 3
```

Alajaotuskilp on korraga tarbija ja vooluallikas: sellel on üks ülesvoolu toide ning mitu allavoolu väljundit. Koormus peab liikuma läbi kogu ahela üles põhikilbini.

### Soovituslik domeenimudel

Füüsiline toide ja seadme elektriline määrang tuleb käsitleda eraldi:

- igal `Equipment` elemendil on püsiv ID;
- objektil on vaikimisi toite viide;
- seade kas pärib objekti vaiketoite või viitab teisele toitele;
- üks füüsiline toide ühendab ühe allika väljundi ühe tarbiva objekti või alajaotuskilbiga ning sisaldab kaabli tüüpi, trajektoori, märkmeid ja silti;
- mitu sama objekti seadet võivad kasutada sama füüsilist toidet ilma kattuvate kaablijoonte dubleerimiseta;
- alajaotuskilbil on ülesvoolu toide ja oma väljundid;
- koormusarvutus summeerib seadmed nende tegeliku toite järgi ning arvutab alajaotuskilbi sisendkoormuse rekursiivselt.

Praegune `PowerConnection` seob ühe allika terve tarbiva objektiga, sisaldab samal ajal ka kaabliandmeid ning sellel on edasise mudeli eeltingimusena püsiv ID. Enne muutmist tuleb otsustada, kas see areneb füüsilise toite mudeliks või asendatakse selgemalt nimetatud mudeliga. Eelistatud on eraldi füüsilise toite mõiste, millele seadmed viitavad.

### Tagasiühilduvus

- Versioonita ja versioon 1 failid peavad edasi avanema.
- Praeguse versioon 2 faili üks ühendus teisendatakse laadimisel objekti vaiketoiteks ning kõik seadmed pärivad selle.
- Vana faili avamine ei tohi faili automaatselt ümber kirjutada.
- Soovituslik on kirjutada uus mudel `.pplan` versioonina 3, sest vanem rakendus ei oskaks seadmepõhiseid ühendusi säilitada ja võiks need vaikides kaotada.
- Versioon 3 võib säilitada sama ZIP-paketistruktuuri, lisades plaaniandmetesse seadmete ID-d, toited ja alajaotuskilbid.

### Ohutusreeglid

- Alajaotuskilpi ei saa ühendada iseendaga.
- Toiteahelas ei tohi tekkida tsüklit.
- Puuduv või kustutatud allikas, väljund, toide või seade peab andma kontrollitava tulemuse, mitte katkestama plaani avamist.
- Väljundi koormus peab arvestama ainult selle kaudu tegelikult toidetavaid seadmeid.
- Üks seade ei tohi korraga kasutada mitut toidet, kuni mitmefaasilise või redundantse toite jaoks pole eraldi nõuet.

### Teostusetapid

1. Kirjeldada uued domeenitüübid ja migratsioonireeglid automaattestidega.
2. Lisada seadmetele püsivad ID-d ning vaiketoite ja erandi lahendamine.
3. Lisada alajaotuskilp ja tsüklivaba üles-/allavoolu koormusarvutus.
4. Laiendada `.pplan` lugemist ja kirjutamist koos versioon 1/2 migratsioonitestidega.
5. Lisada kasutajaliideses objekti vaiketoide ning seadmereal valitav erand.
6. Uuendada kaablite joonistamine, kokkuvõtted, TXT/PDF-eksport ja külgpaneel.

Teise etapi eeldustest on teostatud seadme ja vooluühenduse püsivad ID-d. `Equipment` saab domeenimudelis kasutada objekti vaiketoidet või viidata erandina kindlale `PowerConnection` ID-le. `EventPlan` lubab määrata ainult samale tarbivale objektile kuuluva ühenduse ning eemaldatud ühenduse viide lähtestatakse vaiketoitele. Seadme püsiv ID salvestub valikulise väljana ka versioon 2 faili ning vanema ID-ta seadme laadimisel luuakse ID automaatselt. Seadmepõhise toiteviite salvestamine lisatakse koos versioon 3 vorminguga.

### Vastuvõtukriteeriumid

- Kõik seadmed kasutavad vaikimisi objekti vaiketoidet.
- Üksiku seadme saab suunata teise kilbi konkreetsele väljundile ja hiljem tagasi vaiketoitele.
- Sama objekti eri seadmed jagunevad vähemalt kahe allika vahel õigete võimsustega.
- Alajaotuskilbi sisendkoormus võrdub selle väljundite kaudu toidetava kogukoormusega.
- Põhikilbi koormus sisaldab temast sõltuvate alajaotuskilpide koormust.
- Tsüklilise kilbiahela loomine blokeeritakse selge eestikeelse veateatega.
- Vana v1 ja v2 plaan avaneb sama nähtava elektrijaotusega nagu enne.
- Uus plaan salvestub, avaneb uuesti ning säilitab seadmete erandid, kaablid ja kilbiahela.

## 3. Elektri külgpaneel ja koormusribad

### Kasutusloogika

Praegune tekstikujuline voolukokkuvõte asendatakse või täiendatakse hierarhilise interaktiivse vaatega:

```text
Elektrikapp
└── Pistikupesa 1                         2300 / 3500 W
    [██████████████████░░░░░░░░░░░░]     66%
    ├── Kohvitelk                         1800 W
    └── Valgustus                          500 W
```

Iga elektrikapi juures kuvatakse selle väljundid. Iga väljundi juures kuvatakse kasutatud ja lubatud võimsus, protsent ning värviline koormusriba. Väljundi all kuvatakse sellega ühendatud tarbijad.

Esialgsed värvipiirid:

- alla 70%: roheline;
- 70–89%: kollane;
- 90–100%: oranž;
- üle 100%: punane.

Piirid tuleb enne teostamist kinnitada ning hoida ühes taaskasutatavas arvutus- või kujunduskomponendis.

### Interaktsioonid

- Elektrikapi või tarbija real klõpsamine valib vastava objekti.
- Enter või topeltklõps viib objekti kaardil nähtavale.
- Valitud objekt tõstetakse kaardil selgelt esile.
- Ülekoormatud väljund peab olema eristatav ka ainult teksti ja ikooni järgi, mitte üksnes värviga.
- Olemasolevad `Vool`, `Kaablid` ja `Grupid` filtrid peavad säilitama oma tähenduse või saama selge asenduse.

### Vastuvõtukriteeriumid

- Koormus vastab `PowerSummaryService` arvutustele.
- Nullmahuga ja ühendusteta väljundid ei põhjusta jagamist nulliga ega katkist riba.
- Täpselt 100% ei kuvata ülekoormusena; üle 100% kuvatakse punaselt.
- Tarbija valimine töötab telgi, ala ja joone puhul.
- Vana tekstiraport, PDF-eksport ja `.pplan` vorming ei muutu.

## 4. Objekti kiirotsing

### Kasutusloogika

- Kaks kiiret Shift-klahvi vajutust avavad otsingu, sarnaselt IntelliJ IDEA otsingule.
- Otsing kasutab olemasoleva objektinimekirja nime, tüübi ja grupi filtreerimisloogikat.
- Nooleklahvid muudavad valikut, Enter valib objekti ja Escape sulgeb otsingu.
- Enter tsentreerib kaardi objektile ning kuvab selle ümber lühikese pulseeriva kontrastse raami.
- Otsingu avamisel jäetakse meelde objektipaneeli eelnev avatud või minimeeritud olek ning pärast otsingu lõpetamist taastatakse see.

### Tehnilised piirid

- Kahekordse Shift-klahvi tuvastus peab töötama kogu rakenduse ulatuses.
- Tavaline Shift-klahvi kasutamine tekstiväljades ei tohi otsingut kogemata avada.
- Korduv Enter või kiiresti järjest tehtud otsing ei tohi jätta vanu esiletõste animatsioone kaardile.
- Peidetud objekti otsingutulemus peab näitama, et objekt või selle kiht on peidetud; nähtavust ei tohi vaikimisi püsivalt muuta.

### Vastuvõtukriteeriumid

- Otsing leiab objekti nime, tüübi ja grupi järgi.
- Enter viib nähtava objekti juurde ja esiletõste kaob automaatselt.
- Escape ei muuda valitud objekti.
- Objektipaneeli eelnev olek taastub.

## 5. Suumi kasutuskogemus

### Kasutusloogika

- Tööriistariba `+` ja `-` nupud asendatakse suumiliuguriga.
- Liuguri kõrval säilib täpne protsendinäit ja kiire 100% taastamise võimalus.
- `Alt + hiirerull üles` suurendab ning `Alt + hiirerull alla` vähendab suumi.
- Tavaline hiirerull jätkab kaardiala kerimist.

Rakenduse praegune suumivahemik 25–400% säilib. Liugur peab kasutama sama `setZoom` loogikat, et suumitud kaardi servad jääksid ligipääsetavaks.

### Vastuvõtukriteeriumid

- Liugur, protsendinäit, 100% taastamine ja `Alt + hiirerull` püsivad omavahel sünkroonis.
- Suum ei lähe alla 25% ega üle 400%.
- Tavaline kerimine ja kaardi lohistamine säilitavad praeguse käitumise.
- Windowsi ja Linuxi klaviatuuri- ning hiirekäitumist kontrollitakse eraldi.

## 6. Arenduspõhimõtted

- Iga ülaltoodud tervik tehakse eraldi väikeste commit'ide jadana.
- Domeeniarvutused jäävad `planner-core` moodulisse ja JavaFX-i esitlus `planner-gui` moodulisse.
- Olemasolevaid versioonita, versioon 1 ja versioon 2 `.pplan` faile peab saama edasi avada.
- Kasutajaliidese muudatus ei tohi vaikimisi muuta plaani salvestatud andmeid.
- Automaatkontrollid tehakse taustal; graafiline käsitsi kontroll antakse kasutajale lühikese kontrollnimekirjana.
- Pärast iga sammu käivitatakse `./gradlew clean test` ja `git diff --check`.
