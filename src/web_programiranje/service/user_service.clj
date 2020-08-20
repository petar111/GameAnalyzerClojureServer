(ns web-programiranje.service.user-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]))


(defn get-user-followers-usernames-by-user-id
  "docstring"
  [user_id]
  (db-service/get-user-followers-usernames-by-user-id user_id)
  )

(defn get-user-following-usernames-by-user-id
  "docstring"
  [user_id]
  (db-service/get-user-following-usernames-by-user-id user_id)
  )
