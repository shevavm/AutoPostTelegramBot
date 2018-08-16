# Auto Post Telegram Bot Changelog

## 0.3.2

* Deprecate old remove post messages mechanism and add more informative mechanism
of notifying about removed messages
* Add extension for `BroadcaseChannel` subscribing for more comfortable interact
with events channels

## 0.3.3

* Hotfix for problem with new extension for BroadcastChannels

## 0.3.4

* Changed return type of forwarders
* Now you can subscribe onto post published events

## 0.3.5

* Fixed problem with scheduler on initialization (without any event scheduler
will not update job, but now fixed)

## 0.3.6

* Add `BroadcastReceiver#debounce`

## 0.3.7

* Fix `TimerTriggerStrategy` calculating delay and put trigger work in
different async block

## 0.3.8

**BREAK CHANGES**

* Add `PostsUsedTable`, now any plugin or action which can potentially publish
recommended to register/unregister in this table
* Rename methods of `PostsLikesMessagesTable` to be consistent with their
behaviours
* ***Now plugins `HAVE NO VERSIONS` and must not implement `onInit` method***

## 0.3.9

* Add `Throwable#collectStackTrace` which return as string stacktrace of
caller