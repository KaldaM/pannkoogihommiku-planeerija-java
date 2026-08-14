# Pannkoogihommiku planeerija: eesmärgid, areng ja hetkeseis

- Dokumendi viimane sisuline uuendus: 15. august 2026
- Koodi viimane dokumenteeritud commit: `00161b4` (`Version the plan file format`, 15. august 2026)
- Projekti versioon: `0.1.0`

## 1. Dokumendi eesmärk

See dokument on projekti ühine lähtepunkt kolmel otstarbel:

1. anda bakalaureusetöö jaoks ülevaade probleemi kujunemisest, valikutest ja tehtud tööst;
2. säilitada projekti mälu ka siis, kui senine arendusvestlus pole enam kättesaadav;
3. aidata uuel arendajal või uuel vestlusel kiiresti aru saada, mis on valmis, mis pooleli ja mida tasub järgmisena teha.

Dokumenti tuleks uuendada pärast suurema funktsionaalse terviku valmimist või enne pikemat arenduspausi. Commitide ajalugu jääb detailseks tehniliseks logiks; see fail selgitab muudatuste tähendust ja omavahelisi seoseid.

## 2. Projekti taust

Projekt sai alguse Pythonis loodud pannkoogihommiku planeerijast, mille põhiprogramm oli sisuliselt ühes failis. Java versiooni loomise eesmärk ei olnud ainult olemasoleva programmi ümberkirjutamine, vaid selle valdkonna modelleerimine objektorienteeritult ning sellise struktuuri loomine, mida saaks kasvatada märksa suuremaks ürituste planeerimise rakenduseks.

Struktuurilise eeskujuna kasutati varasemat tarkvaratehnika Java projekti, kus domeeniloogika ja kasutajaliides olid eristatavad. Pannkoogihommik on esimene päriselt kasutatav juhtum: piisavalt konkreetne, et kasutusprobleemid tuleksid kiiresti välja, kuid piisavalt mitmekesine, et katsetada kaarti, objekte, elektriühendusi, kaableid, salvestamist ja eksporti.

Pikemas vaates on projekt bakalaureusetöö prototüüp ja õppematerjal. Praegune töölauarakendus aitab välja selgitada:

- milliseid mõisteid ürituse alaplaani domeen vajab;
- millised töövood on kaardipõhises kasutajaliideses loomulikud;
- kuidas siduda ruumiline plaan elektrivõrgu andmetega;
- millised andmed peavad säilima ja olema teistele eksporditavad;
- mida tuleks suuremas süsteemis arhitektuuriliselt teisiti teha.

## 3. Algsed eesmärgid

### 3.1 Pannkoogihommiku planeerija põhieesmärk

Rakendus peab võimaldama koostada ürituse alaplaani, kus kasutaja saab:

- valida või laadida aluskaardi;
- paigutada kaardile telke ja elektrikappe;
- lisada telkidesse voolu vajavaid seadmeid;
- ühendada telgi konkreetse elektrikapi konkreetse väljundiga;
- näha väljundite ja kappide koormust ning vaba võimsust;
- eristada telke värvi ja grupi järgi;
- kujundada kaablite tegelikke trajektoore;
- salvestada töö ning hiljem samast kohast jätkata;
- eksportida plaan teistele arusaadavasse vormi.

Oluline lõppnõue on see, et tavakasutaja ei peaks rakenduse käivitamiseks paigaldama IntelliJ IDEA-t, Javat ega kasutama käsurida. Selleks on tulevikus vaja Windowsi paigalduspaketti koos rakenduse enda Java runtime'iga.

### 3.2 Bakalaureusetöö suurem visioon

Pannkoogihommiku planeerija kohal on laiem ürituste tehnilise ja korraldusliku planeerimise süsteemi idee.

Alus võiks olla kas:

- kaardilt valitud ja lukustatud geograafiline asukoht;
- kasutaja laaditud skeem või pilt, mille mõõtkava kalibreeritakse teadaoleva vahemaa järgi.

Plaanile kavandatud tehnilised objektid:

- elektriallikad ja elektrikilbid eri tüüpi ning eri arvu väljunditega;
- telgid muudetava suuruse, pöörde, värvi, seadmete ja elektriühendustega;
- alajaotuskilbid, mis on korraga tarbijad ja uued vooluallikad;
- kõlarid suuna, tüübi, võimsuse ja vooluvajadusega;
- eraldiseisvad elektritarbijad;
- 230 V, 16 A, 32 A ja 63 A kaablid muudetava trajektoori ning kaablijuppide loeteluga;
- helipuldid ja XLR-kaablid;
- vabalt seadistatavad kujundid ja märkmed.

Korraldajale kavandatud objektid ja võimalused:

- vabakujulised alad ehk polygonid, näiteks publiku- või toitlustusalad;
- vabakujulised jooned ehk polüjooned, näiteks aiad ja liikumistrajektoorid;
- WC, turva, saun või tünnisaun, infotelk ning start/finiš;
- tekstikastid;
- liikmed koos ülesannetega;
- sponsoraiad, bännerid ja lipud;
- lihtne korraldajavaade ning eraldi tehniline vaade.

Veel kaugemad ideed:

- Tartu linna aluskaartide või linnavõrgu andmete otsene kasutamine;
- plaani ainult vaatamiseks mõeldud veebivaade;
- kasutajakontod ja organisatsioonid;
- festivalide ning nende alaplaanide kaustastruktuur;
- administraatori- ja muutmisõigused;
- erilahendus pubiralli trajektooride planeerimiseks;
- hele ja tume kujundus.

Need ei ole praeguse prototüübi lubatud funktsioonid, vaid suurema süsteemi ulatus, mille jaoks prototüüp teadmisi kogub.

## 4. Arenduspõhimõtted

Arendus on toimunud teadlikult väikeste sammudena. Üks kasutaja jaoks kontrollitav muudatus tehakse valmis, käivitatakse, proovitakse päris plaanil ning commititakse eraldi. Selline tööviis on seni andnud 185 commiti ja võimaldab näha, miks iga funktsioon lisati või ümber tehti.

Olulisemad kujunenud põhimõtted:

- kasutaja päris töövoog on olulisem kui esialgne tehniline lahendus;
- juba salvestatud vanad plaanid peavad pärast andmemudeli täiendamist edasi avanema;
- sagedased tegevused peavad vajama võimalikult vähe dialooge ja kinnitusi;
- harva muudetavad plaaniülesed valikud kuuluvad eraldi „Plaani andmed” dialoogi;
- kaardil peab saama objekte valida ja muuta ilma külgpaneeli tarbetu hüppamise või ülerahvastamiseta;
- lukustus kaitseb objekti asukohta, kuid ei tohi keelata selle andmete muutmist;
- automaatne rakendamine sobib lihtsatele tekstiväljadele, kuid ohtlik tegevus vajab kinnitust;
- kaardil olev tekst peab olema loetav nii tavakaardil kui ortofotol;
- keerukad kujundid luuakse punktide järjestikuse lisamisega ning neid muudetakse pärast loomist samade punktide kaudu.

## 5. Tehniline ülesehitus

### 5.1 Tehnoloogiad

- Java 21 LTS
- JavaFX 21.0.4
- Gradle Wrapper ja mitme mooduliga Gradle'i projekt
- Apache PDFBox 2.0.31 PDF-raportite loomiseks
- JUnit Jupiter 5.10.2 testide taristuna

Java 21 valiti pika toe ja JavaFX 21-ga stabiilse sobivuse tõttu. Projekt ei vaja praegu Java 25 funktsioone; hilisem uuendamine on võimalik eraldi kontrollitud muudatusena.

### 5.2 Moodulid

`planner-core` sisaldab rakenduse domeenimudelit ja teenuseid:

- plaani objektid ja nende ühised omadused;
- elektriallikad, väljundid, tarbijad ja ühendused;
- koormuste kokkuvõtted;
- `.pplan` faili salvestamine ja avamine.

`planner-gui` sisaldab JavaFX-i kasutajaliidest:

- tööriistariba, külgpaneelid ja dialoogid;
- kaardi renderdamine, suumimine ja nihutamine;
- objektide ning nende siltide interaktsioonid;
- mõõdulint ja trajektooride redaktorid;
- TXT-, PNG- ja PDF-eksport.

`planner-gui` sõltub Gradle'is tavapäraselt `planner-core` moodulist. Core kompileeritakse eraldi teegiks ning selle lähtekoode ei kaasata GUI moodulisse teist korda.

Ekspordi- ja kaabliloogikat on peamisest kasutajaliidese klassist juba eraldi abiklassidesse tõstetud. Sellest hoolimata on `PancakePlannerApp` endiselt väga suur ning vajab edasise kasvu eel vaadeteks, kontrolleriteks ja tööriistadeks jagamist.

### 5.3 Olulisemad domeeniklassid

| Klass | Vastutus |
| --- | --- |
| `EventPlan` | Plaani nimi, kaart, mõõtkava, objektid, vooluühendused ja kihtide nähtavus |
| `PlannerObject` | Kõigi objektide ID, nimi, asukoht, grupp, märkmed, lukustus ja nimesilt |
| `Tent` | Mõõtmed, pööre, värv, seadmed ja summaarne vooluvajadus |
| `PowerSource` | Elektrikapp ja selle väljundite loetelu |
| `PowerOutlet` | Nimi, ühenduse tüüp ja lubatud võimsus |
| `PowerConnection` | Allikas, tarbija, väljund, kaabli tüüp, märkmed, jupid ja trajektoor |
| `CustomObject` | Ristkülik või ring muudetava suuruse, pöörde, värvi ja läbipaistvusega |
| `TextObject` | Mitmerealine tekstikast, pealkiri, värv ja kirjasuurus |
| `MarkerObject` | Ikooniga objekt, näiteks WC, saun, liige, turva või start/finiš |
| `AreaObject` | Punktidest koosnev värviline ja läbipaistev ala |
| `LineObject` | Punktidest koosnev vabakujuline joon |

### 5.4 Elektrimudel

Voolutarbijad on `Tent`, `AreaObject` ja `LineObject`. Nende seadmete võimsused liidetakse ning objekt ühendatakse ühe elektrikapi ühe konkreetse väljundiga. Väljundi koormus arvutatakse selle külge ühendatud objektide võimsustest. Kõigil kolmel tarbijatüübil saab kaardil määrata füüsilise vooluühenduse punkti.

Toetatud ühendused ja algsed vaikemahud:

| Ühendus | Vaikemaht |
| --- | ---: |
| 230 V tavapesa | 3500 W |
| 16 A tööstusvool | 11000 W |
| 32 A tööstusvool | 22000 W |
| 63 A tööstusvool | 43500 W |

Need väärtused on planeerimise praktilised vaikeväärtused, mitte elektriprojekti asendus. Kasutaja saab iga väljundi nime, tüüpi ja mahtu muuta. Ülekoormus tuuakse kokkuvõttes nähtavalt esile.

### 5.5 Salvestusvorming

Plaan salvestatakse `.pplan` laiendiga Java properties-vormingus faili. Salvestatakse muu hulgas:

- plaani nimi, kaardi viide ja mõõtkava;
- siltide kirjasuurused;
- kõik objektid ning nende tüübipõhised omadused;
- grupid, lukud, märkmed ja nimesiltide asukohad;
- elektriväljundid ja vooluühendused;
- kaablite trajektoorid, märkmed, jupid ja sildiasukohad;
- kihtide, kaablitüüpide ja gruppide nähtavus.

Uued failid sisaldavad täisarvulist `formatVersion` välja; praegune vorminguversioon on `1`. Versioonita fail loetakse tagasiühilduvuse huvides esimese versiooni failiks. Rakendus keeldub endast uuema vormingu avamisest ja palub kasutajal rakendust uuendada, selle asemel et tundmatuid andmeid vaikselt valesti tõlgendada. Eraldi migratsioonisüsteemi ei ole veel vaja läinud, kuid see tuleb lisada enne esimest murdvat vormingumuudatust.

Kasutaja laaditud kaart salvestatakse praegu failiteena. Seetõttu võib plaan koos kaardiga teise arvutisse viimisel kaardi kaotada. Tulevikus tuleks kaart kas plaanifaili sisse pakkida või kasutada plaanikausta suhtelist teed.

## 6. Praeguseks saavutatud funktsionaalsus

### 6.1 Plaan ja aluskaart

- Rakendus alustab tühja plaaniga ja avaneb maksimeeritud aknas.
- Kasutada saab projektiga kaasas olevat tavakaarti ja ortofotot.
- Kasutaja saab laadida oma PNG- või JPEG-kaardi.
- Plaanile saab anda nime; nimi on rakenduses nähtav.
- Mõõtkava saab sisestada pikslite arvuna meetri kohta või määrata mõõdulindi järgi.
- Mõõtkava muutmisel uuenevad olemasolevate mõõdulintide näidud.
- Kaarti saab suumida, nihutada ja taastada 100% suurusele.
- Suumitud kaardi kõik servad on ligipääsetavad.

### 6.2 Objektide lisamine ja haldamine

- Telgid, elektrikapid, tavaobjektid, tekstikastid, ikoonmarkerid, jooned ja alad.
- Lisamisel küsitakse samas dialoogis objekti nimi, grupp ning tüübile vajalikud põhiomadused.
- Objekti lisamine talub hiire väikest liikumist, et kõrge DPI ei muudaks klõpsu kogemata kaardi lohistamiseks.
- Objekte saab valida kaardilt või objektide nimekirjast.
- Nimekirjast valitud objekt tuuakse kaardil nähtavale ja selle juurde saab vaate tsentreerida.
- Objekte saab liigutada, dubleerida ja kinnitusega kustutada.
- Lukustatud objekti ei saa liigutada ega kustutada, kuid selle andmeid saab muuta.
- Objekte saab gruppidesse määrata ja gruppide nähtavust muuta.
- Objekti nimesilti saab eraldi peita, lohistada ja vaikeasukohta taastada.
- Kõik objektisildid saab plaani kihina korraga välja lülitada.
- Siltidel on kontrastne taust, et need oleksid loetavad ka ortofotol.

### 6.3 Telgid ja seadmed

- Telgi laius, pikkus, pööre ja värv on muudetavad; läbipaistvust saab määrata slideriga.
- Telki saab lisada nime ja võimsusega seadmeid ning neid eemaldada.
- Telgi vooluvajadus arvutatakse seadmete summana.
- Telgi dubleerimisel kopeeritakse seadmed, kuid mitte elektriühendus.

### 6.4 Elektrikapid ja ühendused

- Kappi saab lisada eri tüüpi väljundeid.
- Väljundil on muudetav nimi, ühenduse tüüp ja võimsus.
- Ühendatud väljundi muutmisel või eemaldamisel kaitsevad hoiatused olemasolevaid seoseid.
- Telgi saab ühendada konkreetse väljundiga külgpaneelist või valida kapi otse kaardilt.
- Kui sobiva tüübiga väljund on üheselt valitav, saab süsteem selle automaatselt määrata.
- Valikus näidatakse ainult konkreetses kapis päriselt olemas olevaid ühendusetüüpe ja väljundeid.
- Koormust arvestatakse väljundi, mitte ainult kogu kapi tasemel.
- Ülekoormatud väljundid on kokkuvõttes nähtavad.

### 6.5 Voolukaablid

- Ühendatud telgi ja kapi vahel kuvatakse kaabel.
- Kaabli tüüp tuleneb valitud elektriühendusest.
- Kaablile saab lisada vahepunkte ning kujundada tegeliku trajektoori.
- Vahepunkte saab reaalajas lohistada, lõigule lisada ja paremklõpsuga eemaldada.
- Punktide muutmine ei nihuta samal ajal kaardivaadet.
- Kaabli tegelik pikkus arvutatakse mõõtkava järgi.
- Valitud ühenduse tarbijapoolset ühenduspunkti saab kaardil lohistada; kaabel ja pikkus uuenevad juba lohistamise ajal.
- Ühenduspunkt paikneb objekti suhtes, liigub objektiga kaasa ning selle saab paremklõpsuga keskpunkti lähtestada.
- Eraldi saab märkida olemasolevate kaablijuppide kombinatsiooni, näiteks `20 m + 10 m + 10 m`.
- Kaablisildid on lühikesed, lohistatavad, peidetavad ja lähtestatavad.
- Kaableid saab filtreerida 230 V, 16 A, 32 A ja 63 A tüübi järgi.
- Kokkuvõte koondab kaablite pikkused ja jupid tüübi kaupa.

### 6.6 Vabakujulised jooned ja alad

- Joon luuakse kaardile järjest punkte lisades.
- Ala luuakse vähemalt kolmest järjest lisatud punktist.
- Loomise ajal kuvatakse kujuneva objekti eelvaade.
- Loomise saab lõpetada nupu või Enter-klahviga ja katkestada Escape-klahviga.
- Olemasolevaid punkte saab lohistada ja paremklõpsuga eemaldada.
- Punktide vahel kuvatakse väiksemad vahepunktid; nende lohistamisel lisatakse kujundisse uus pärispunkt.
- Lukustatud joone või ala geomeetriat muuta ei saa.
- Ala värvi saab muuta ning läbipaistvust saab määrata slideriga.
- Joone värvi ja paksust saab määrata nii loomisel kui hiljem; paksust muudetakse slideriga.

See funktsionaalsus on värskelt lisatud ja vajab enne uute objektitüüpide juurde liikumist veel terviklikku kasutuskatset.

### 6.7 Korraldaja objektid

- Tavaobjekti saab näidata ristküliku või ringina ning muuta selle mõõtmeid, pööret ja värvi; läbipaistvust saab määrata slideriga.
- Tekstiobjektil on rasvases kirjas pealkiri, mitmerealised märkmed, värv ja slideriga muudetav kirjasuurus.
- Plaani objekti- ja kaablisiltide üldist kirjasuurust saab muuta slideritega „Plaani andmed” dialoogis.
- Markerid kasutavad teksti asemel eristatavaid ikoone.
- Olemas on vähemalt WC, turva, start/finiš, saun/tünnisaun ja liikme tüübid.
- Objektide nimekiri näitab objektide värve ning toimib seetõttu ka lihtsa legendina.

### 6.8 Kihid, külgpaneel ja kokkuvõtted

- Objektitüüpe, silte, kaableid, kaablitüüpe ja gruppe saab eraldi näidata või peita.
- Kihtide nähtavus säilib plaani salvestamisel ja avamisel.
- Kõik kihid saab korraga sisse või välja lülitada.
- Objektiloend asub detailide kohal ning selle kõrgust saab lohistades muuta.
- Objektiloendi kasutaja valitud kõrgus jäetakse rakenduste vahel meelde.
- Külgpaneel kuvab ainult valitud objektitüübile asjakohaseid välju.
- Telgi seadmed ja kapi väljundid paiknevad vastava objekti detailide juures.
- Voolu-, kaabli- ja grupikokkuvõtteid saab ükshaaval sisse ja välja lülitada.

### 6.9 Salvestamine ja eksport

- Uue tühja plaani loomine.
- Plaani salvestamine, „Salvesta kui” ja olemasoleva `.pplan` faili avamine.
- Viimati kasutatud faili ja kausta meelespidamine.
- Salvestamata muudatuste nähtav olek.
- Enne uue plaani loomist, teise plaani avamist või rakenduse sulgemist pakutakse muudatuste salvestamist.
- Varem loodud plaanifailid on püsinud uute versioonidega avatavad.
- Tekstiraporti eksport.
- Kaardipildi eksport PNG-na valitava ulatusega.
- PDF-eksport koos ühes dialoogis valitavate sisu- ja kompaktsusvalikutega.
- Eksporditavate failide nimed tuletatakse ühtlaselt plaani nimest.

## 7. Arenduse kronoloogia

Allolev ajajoon koondab 185 commitist tähenduslikud etapid. Täpne muudatuste loetelu on käsuga `git log --reverse --oneline`.

### 1. juuli 2026: alus ja esimene töötav vertikaallõige

- Loodi Gradle'i mitme mooduliga JavaFX-i projekt.
- Eraldati domeenimudel ja kasutajaliides.
- Lisati objektide detailpaneel, telgi värv ja seadmed.
- Loodi esimene vooluallika valik ja tarbijate kokkuvõte.
- Lisati kaardipildi laadimine ning vaikimisi ortofoto.

### 2. juuli 2026: põhiplaneerija valmimine

- Lisati kustutamine, lukustamine, telgi mõõtmed ja pööre.
- Valmis mõõdulint, suum ning nende esimesed parandused.
- Lisati elektriühendused, kaablipikkus ja kaardilt kapi valimine.
- Lisati grupid, kaardisildid ja grupifiltrid.
- Nähtavusseaded hakkasid plaanis säilima.
- Lisati tekstieksport, plaani nimi ja uus tühi plaan.
- Telke ja kappe sai hakata kaardiklikiga lisama.
- Elektrikapi mudel arenes konkreetsete nimetatud väljundite, ühendusetüüpide ja ülekoormuse arvestuseni.
- Lisati turvalisem salvestamata muudatuste käsitlus.

### 3.–4. juuli 2026: üldobjektid ja külgpaneeli korrastamine

- Parandati eestikeelse teksti kodeeringut.
- Lisati üldised ruudu- ja ringikujulised objektid.
- Nende välimus, suurus ja pööre muutusid seadistatavaks.
- Külgpaneel jagati tüübipõhisteks ja kokkupandavateks osadeks.
- Kokkuvõtteid sai eraldi sisse ja välja lülitada.

### 6. juuli 2026: kaablite esimene tervik

- Elektriühendused muutusid kaardil nähtavateks kaabliteks.
- Lisati kaabli- ja sildikihid, legend ning valiku esiletõstmine.
- Lisati kaablimärkmed ja kaablijuppide kirjeldus.
- Tekstiväljade muudatused hakkasid sobivates kohtades automaatselt rakenduma.
- Tööriistariba ja salvestusolek muudeti selgemaks.
- Kaablikokkuvõte hakkas arvutama pikkusi ja koondama juppe tüübi järgi.

### 12.–13. juuli 2026: päris kaablitrajektoor ja mõõtkava

- Lisati 63 A tööstusvool.
- Kaablile lisati muudetavad vahepunktid.
- Punktide lohistamine muutus reaalajas nähtavaks ning punktide lisamine ja eemaldamine mugavaks.
- Kaablimärkmed ja pikkuse märkmed eraldati.
- Lukustatud objektide kustutamine blokeeriti.
- Lisati plaani muudetav mõõtkava ja mõõdulindi järgi kalibreerimine.
- Plaani nimi, kaart ja mõõtkava viidi „Plaani andmed” dialoogi.

### 17. juuli 2026: lisamisvoog, markerid ja sildid

- Objektide lisamine koondati ühtsesse töövoogu.
- Enne paigutamist sai ühes dialoogis valida nime, grupi, värvi ja tüübipõhised omadused.
- Kõrge DPI-ga hiire väike liikumine ei hakanud enam klõpsamist rikkuma.
- Tekstiobjekt eraldati tavaobjektist.
- Lisati ikoonmarkerid ning markeritüüpide vaikevärvid.
- Kaardi- ja kaablisildid tehti loetavamaks ning lohistatavaks.

### 20.–21. juuli 2026: objektide haldus, eksport ja koodi tükeldamine

- Lisati objektide dubleerimine ja tüübipõhised kihid.
- Valmis värvide ning peidetud olekuga objektide nimekiri.
- Nimekirja kõrgus muutus kasutaja poolt lohistatavaks ja püsivaks.
- Lisati nimesiltide üldine kiht ja tekstiobjekti kirjasuurus.
- Plaani seadistustesse lisati objekti- ja kaablisiltide kirjasuurused.
- Valmis PNG-, täiustatud TXT- ja PDF-eksport.
- Ekspordi ning kaablite kuvamise loogikat hakati `PancakePlannerApp` klassist eraldi klassidesse tõstma.

### 24. juuli 2026: vabakujulised jooned ja alad

- Loodi ala- ja jooneobjektide domeenimudelid ning salvestamine.
- Lisati vastavad kaardikihid ja renderdamine.
- Punktide järjestikuse klõpsamisega lisamisvoog asendas ebaloomuliku valmis algkujundi.
- Lisati punktide lohistamine, eemaldamine ja vahepunktide kaudu juurde tekitamine.
- Viimase commitiga sai pärast loomist muuta joone värvi.

## 8. Kasutajatestides tehtud olulisemad õppetunnid

Projekti väärtus ei ole ainult funktsioonide arvus. Korduv päris kasutamine tõi välja mitu üldistatavat disainiõppetundi:

- Kaardi klõpsu ja lohistamise eristamiseks on vaja liikumislävendit; vastasel juhul sõltub töökindlus hiire DPI-st.
- Suum peab säilitama ligipääsu kogu sisule, mitte ainult suurendama vaadet keskpunkti ümber.
- Ajutine tööriist peab pärast oma tegevuse lõpetamist välja lülituma, et järgmine klõps ei teeks ootamatut muudatust.
- Pidevalt nähtav külgpaneel ei tohi sisaldada iga objektitüübi kõiki välju korraga.
- Ühe objekti lisamiseks ei tohiks kasutaja läbida mitut järjestikust dialoogi.
- Kaardi ortofoto nõuab siltidelt tausta või muud kontrasti tagavat lahendust.
- Suurte ja tihedate plaanide korral peavad sildid olema peidetavad ning ümberpaigutatavad.
- Vabakujulise geomeetria lõigule klõpsamine lisab liiga kergesti kogemata punkti; väiksemad lohistatavad vahepunktid annavad kavatsusest selgema signaali.
- Lukustamine tähendab asukoha kaitsmist, mitte kogu objekti muutumatuks tegemist.
- Salvestusvormingu tagasiühilduvus on kasutaja usalduse jaoks keskne funktsioon, mitte kõrvaline tehniline detail.

Need tähelepanekud sobivad bakalaureusetöös kasutajakeskse iteratiivse arenduse näideteks.

## 9. Pooleliolev ja järgmised tööd

### 9.1 Vahetu jätkamiskoht

Joonte ja alade geomeetria ning põhilised visuaalsed omadused on valmis. Telk, ala ja joon kasutavad ühist `EquipmentContainer` lepingut, nende seadmeid saab hallata sama külgpaneeli kaudu ning kõiki kolme saab kasutajaliideses ühendada elektrikapi väljundiga. Kaardikaablid, vahepunktid, kokkuvõtted ja tekstiaruanded töötavad kõigi kolme tarbijatüübiga. `PowerConnectable` mudel hoiab keskpunkti suhtelist ühenduspunkti nihet ning see salvestub tagasiühilduvalt. Valitud ühendusel kuvatakse lohistatav tarbijapoolne ühenduspunkt, mille muutmisel uueneb kaabel reaalajas. Vahetu järgmine samm on selle tervikvoo käsitsi kontrollimine telgi, ala ja joonega, sealhulgas objekti liigutamise, salvestamise ning uuesti avamise järel.

### 9.2 Joonte ja alade järgmine funktsionaalne etapp

Kasutaja on selgelt määranud järgmised nõuded:

- ala ja joone sisse peab saama lisada seadmeid;
- ala ja joon peavad saama olla elektritarbijad;
- joone elektriline kasutusjuht võib olla näiteks valguskett;
- ala elektriline kasutusjuht võib olla ebakorrapärase kujuga lava;
- voolukaabel ei tohi selliste objektide puhul vaikimisi ühineda geomeetrilise keskpunktiga;
- ka telgil peab saama määrata, millisest kohast vool füüsiliselt ühendub.

Seda ei tasu lahendada kolme eraldi erandina. Enne kasutajaliidese lisamist tuleks kujundada ühine mudel elektrit tarbivate ja seadmeid sisaldavate objektide jaoks. Võimalik suund on eraldada:

- seadmete hoidmise võime;
- arvutatav vooluvajadus;
- kasutaja määratav elektriühenduse ankrupunkt.

`Tent`, `AreaObject` ja `LineObject` kasutavad sama seadmete ning vooluvajaduse lepingut. Salvestamine, ühendused, kaardikaablid ja kokkuvõtted on üldistatud. Ühenduspunkti nihe on mudelis ja failivormingus olemas ning seda saab valitud ühendusel kaardil lohistada või lähtestada.

### 9.3 Ühenduspunkti järelkontroll

- Kontrollida ühenduspunkti muutmist telgil, alal ja joonel.
- Kontrollida, et punkt liigub objekti ning muudetud joone- või alageomeetriaga ootuspäraselt kaasa.
- Kontrollida punkti lähtestamist ning säilimist plaani salvestamisel ja avamisel.

### 9.4 Kvaliteet ja arhitektuur

Enne funktsioonide hulga suurt kasvatamist on vaja:

- jagada väga suur `PancakePlannerApp` väiksemateks vaate-, kontrolleri- ja tööriistaklassideks;
- lisada domeeniloogika automaattestid;
- lisada salvestamise ja vanade failide tagasiühilduvuse testid;
- lisada kaablite ning kujundite geomeetria testid;
- lisada esimese murdva `.pplan` vormingumuudatuse eel versioonidevahelised migratsioonid;
- otsustada, kas properties-vorming sobib pikaajaliselt või tuleks liikuda näiteks versioonitud JSON-vormingule;
- lisada logimine ja kasutajale arusaadavad veateated ootamatute failivigade jaoks.

### 9.5 Tavakasutajale väljastamine

See on algse ülesande oluline, kuid seni tegemata osa:

- luua Gradle'i ülesanne Windowsi `jpackage` paketi tegemiseks;
- lisada paketti vajalik Java runtime ja JavaFX;
- luua rakenduse ikoon ning versiooniinfo;
- valida esmalt `app-image` või kaasaskantav pakett kiireks testimiseks;
- seejärel luua tavapärane Windowsi installer;
- kontrollida paigaldust arvutis, kus Javat ja IntelliJ IDEA-t ei ole;
- dokumenteerida paigaldamine, uuendamine ja plaanifailide seos rakendusega.

### 9.6 Suurema süsteemi funktsioonid

Pärast prototüübi põhivoo stabiliseerimist:

- alajaotuskilbid ehk korraga tarbija ja allikas;
- eraldiseisvad elektritarbijad;
- kõlarid, helipuldid ja XLR-kaablid;
- aiad, bännerid ja lipud;
- ülesannetega liikmed ning muud korraldaja objektid;
- hele ja tume režiim;
- tagasivõtmine ja uuestitegemine;
- automaatsalvestus või taastamisfail;
- aluskaartide geograafilised koordinaadid;
- veebis vaadatav avaldatud plaan;
- autentimine, organisatsioonid, festivalid, kaustad ja õigused.

Veebivaade ja organisatsioonid tähendavad tõenäoliselt eraldi serverit, andmebaasi ja veebiklienti. Neid ei ole mõistlik praegusesse JavaFX-i klassi otse juurde kasvatada; bakalaureusetöö arhitektuur peaks käsitlema töölauarakendust ühe võimaliku kliendina.

## 10. Teadaolevad piirangud ja riskid

- Automaattestid katavad geomeetriat, seadmemudelit, salvestamise tagasiühilduvust, vooluarvutust, kaabli otspunkte ja tekstiaruannet, kuid kasutajaliidese sündmuste testikate on endiselt piiratud.
- Peamine JavaFX-i rakendusklass on liiga suur ja koondab veel palju erinevaid vastutusi.
- Salvestusvormingul on versiooninumber, kuid puuduvad veel ametlik skeem ja versioonidevahelised migratsioonid.
- Kasutaja enda kaardipildi viide ei ole teise arvutisse liigutamisel kaasaskantav.
- Undo/redo puudub, mistõttu sõltub vigade parandamine käsitsi muutmisest või varasemast salvestusest.
- Windowsi installeri ja iseseisva runtime'iga väljalaset ei ole.
- Lohistatava ühenduspunkti JavaFX-i hiirekäitumist ei kata automaattest; see vajab käsitsi kontrollimist eri objektitüüpidega.
- Rakendusel ei ole veel veebivaadet, kasutajakontosid, õigusi ega keskset andmehoidlat.
- Tartu kaardiandmetega otseliidestust ei ole.

## 11. Soovituslik tööjärjekord

1. Kontrolli ühenduspunkti lohistamist ja lähtestamist telgil, alal ning joonel.
2. Kontrolli ühenduspunkti liikumist objekti ja geomeetria muutmisel ning säilimist plaani salvestamisel ja avamisel.
3. Paranda käsitsi kontrollimisel ilmnevad vead.
4. Alusta automaatteste salvestamisest ja vooluarvutusest, sest nende regressiooni mõju on suurim.
5. Tükelda `PancakePlannerApp` järk-järgult, alustades ala- ja joone tööriistast või detailpaneelist.
6. Lahenda kaardifailide kaasaskantavus ning lisa vajaduse tekkimisel vormingu migratsioonid.
7. Loo Windowsi proovipakett ning katseta seda puhtas arvutis.
8. Alles seejärel vali bakalaureusetöö järgmine suurem vertikaallõige, näiteks alajaotuskilp või avalik veebivaade.

## 12. Uue arendusvestluse alustamise juhis

Uuele arendajale või tehisintellekti vestlusele tuleks anda vähemalt järgmine info:

> Ava esmalt `README.md` ja `docs/PROJEKTI_ULEVAADE.md`. Vaata `git status --short` ning viimaseid committe käsuga `git log -15 --oneline`. Ära eelda, et dokument on koodist uuem: kontrolli alati praegust teostust. Projektis tehakse üks kasutaja poolt kontrollitav muudatus korraga, see testitakse ning kasutaja commitib selle eraldi. Säilita vanade `.pplan` failide avamine. Ära keela lukustatud objekti andmete muutmist; lukk kaitseb selle asukohta. Telgi, ala ja joone seadmed, vooluühendused, kaardikaablid ning aruanded kasutavad ühist loogikat. Valitud ühenduse tarbijapoolset ankrupunkti saab kaardil lohistada ja paremklõpsuga lähtestada; järgmine samm on selle käsitsi kontrollimine eri objektitüüpide, objekti liigutamise ning salvestamise järel.

Tavaline kontroll enne muutmist:

```powershell
git status --short
git log -15 --oneline
```

Rakenduse käivitamine:

```powershell
.\gradlew.bat :planner-gui:run
```

Testide käivitamine:

```powershell
.\gradlew.bat test
```

Pärast iga sammu:

```powershell
git diff --check
git status --short
```

Seejärel proovib kasutaja muudatust IntelliJ IDEA kaudu. Kui see töötab, tehakse üks kirjeldava ingliskeelse nimega commit ja lükatakse GitHubi.

## 13. Dokumendi hooldamine

Pärast iga suuremat etappi tuleks uuendada vähemalt:

- dokumendi kuupäeva ja viimase commiti viidet;
- saavutatud funktsionaalsuse peatükki;
- pooleliolevate tööde ning piirangute nimekirja;
- soovituslikku tööjärjekorda;
- uue vestluse vahetut jätkamiskohta.

Commitide üksikasju ei ole vaja siia ükshaaval kopeerida. Eesmärk on säilitada otsused, põhjendused, tervikpilt ja järgmine selge tegevus.
