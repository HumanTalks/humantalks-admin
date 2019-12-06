# Backend for HumanTalks organizers

## TODO

- auth
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

example :

```
heroku config:set SBT_CLEAN=true --app humantalksparis
git push heroku-prod master
heroku config:unset SBT_CLEAN --app humantalksparis
```

## Public Api

This backend has a public Api allowing to retrieve some data.
Here are the endpoints :

- `/meetups` : allow to retrieve published meetups
- `/meetups/:id` : allow to retrieve a meetup with its id
- `/talks` : allow to retrieve published talks
- `/talks/:id` : allow to retrieve a talk with its id
- `/speakers` : allow to retrieve published speakers
- `/speakers/:id` : allow to retrieve a speaker with its id
- `/venues` : allow to retrieve published venues
- `/venues/:id` : allow to retrieve a venue with its id

All endpoints can take an `include` query parameter that embeds required data directly inside the response.

Ex: `/talks?include=speaker,meetup,venue`
