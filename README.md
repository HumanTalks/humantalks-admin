# Backend for HumanTalks organizers

## TODO

- 107 vidéos enregistrés alors qu'il y en a 120 sur youtube... (voir celles qui manquent)
- rentrer l'historique de humantalks.com
- supprimer les personnes en double
- enrichir le profil des personnes (twitter et mail) et des lieux (contact et places)


## Tech

### Auth with silhouette

exemples :

- https://github.com/adrianhurt/play-silhouette-credentials-seed
- https://github.com/ezzahraoui/play-silhouette-reactivemongo-seed
- https://github.com/pariksheet/dribble

Sources :

- https://github.com/mohiva/play-silhouette-persistence-reactivemongo
- http://www.ibm.com/developerworks/library/wa-playful-web-dev-1-trs-bluemix/index.html


## Deploy

When packages changes, heroku may be lost.
If so, use [clean build](https://devcenter.heroku.com/articles/scala-support#clean-builds)

ex :

```
heroku config:set SBT_CLEAN=true --app humantalksparis
git push heroku-prod master
heroku config:unset SBT_CLEAN --app humantalksparis
```
