# Backend for HumanTalks organizers

## TODO

- 107 vidéos enregistrés alors qu'il y en a 120 sur youtube... (voir celles qui manquent)
- rentrer l'historique de humantalks.com
- supprimer les personnes en double
- enrichir le profil des personnes (twitter et mail) et des lieux (contact et places)

## Deploy

When packages changes, heroku may be lost.
If so, use [clean build](https://devcenter.heroku.com/articles/scala-support#clean-builds)

ex :

```
heroku config:set SBT_CLEAN=true --app humantalksparis
git push heroku-prod master
heroku config:unset SBT_CLEAN --app humantalksparis
```
