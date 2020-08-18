(ns web-programiranje.db.config.db-config
  (:require [toucan.db :as db]))

(defn inital-configurantion []
  (db/set-default-db-connection!
    {
     :dbtype "mysql"
     :dbname "game_assistant_v2_clojure"
     :user "root"
     :password ""
     })
  (db/set-default-quoting-style! :mysql)
  )

