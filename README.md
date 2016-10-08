# Backend for HumanTalks organizers

## Deploy

When packages changes, heroku may be lost.
If so, use [clean build](https://devcenter.heroku.com/articles/scala-support#clean-builds)

ex :

```
heroku config:set SBT_CLEAN=true --app humantalksparis
git push heroku-prod master
heroku config:unset SBT_CLEAN --app humantalksparis
```
