# Pannkoogihommiku planeerija

JavaFX-is tehtud pannkoogihommiku plaani koostamise rakendus. Projekti eesmärk on hoida domeeniloogika, salvestamine ja kasutajaliides eraldi, et sama struktuuri oleks hiljem võimalik kasvatada suuremaks ürituse planeerimise süsteemiks.

## Moodulid

- `planner-core` - plaani objektid, vooluarvutused ja salvestamise loogika.
- `planner-gui` - JavaFX kasutajaliides.

## Käivitamine arenduses

```powershell
gradle :planner-gui:run
```

Hiljem saab rakenduse pakkida tavakasutajale sobivaks Windowsi programmiks `jpackage` abil.
