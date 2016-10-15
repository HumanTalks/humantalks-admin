# Backend for HumanTalks organizers

## TODO

- auth
    - link person with user OR merge them ?
    - create or find a person when creating a new user
    - send real emails
    - add user roles
    - add user admin (list all and define roles)
    - improve auth texts
- i18n
- 107 vidéos enregistrés alors qu'il y en a 120 sur youtube... (voir celles qui manquent)
- rentrer l'historique de humantalks.com
- supprimer les personnes en double
- enrichir le profil des personnes (twitter et mail) et des lieux (contact et places)

## Install

- install `java` (1.8.0_91), `scala` (2.11.7), `sbt` (0.13.11) and `mongo` (3.0.2) if they are not already installed
- clone this repo
- start mongo
- run `sbt run` at the root of this repo


## Deploy

When packages changes, heroku may be lost.
If so, use [clean build](https://devcenter.heroku.com/articles/scala-support#clean-builds)

ex :

```
heroku config:set SBT_CLEAN=true --app humantalksparis
git push heroku-prod master
heroku config:unset SBT_CLEAN --app humantalksparis
```

## Tech

### Auth with silhouette

exemples :

- https://github.com/mohiva/play-silhouette-seed
- https://github.com/adrianhurt/play-silhouette-credentials-seed
- https://github.com/ezzahraoui/play-silhouette-reactivemongo-seed
- https://github.com/pariksheet/dribble

Sources :

- https://github.com/mohiva/play-silhouette-persistence-reactivemongo
- http://www.ibm.com/developerworks/library/wa-playful-web-dev-1-trs-bluemix/index.html
