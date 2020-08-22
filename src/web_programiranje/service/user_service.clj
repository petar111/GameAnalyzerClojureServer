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

(defn promote-user [user rank]
  (if (db-service/update-user-rank (:id user) (:id rank))
    (db-service/get-user-by-id (:id user))
    user
    )
  )

(defn get-rank-for-experience [experience]
  (db-service/get-rank-by-experience experience)
  )

(defn ready-for-promotion? [user]
    (let [user-db (db-service/get-user-by-id (:id user))]
      (> (:experience user-db) (:experience_max (:rank user-db)))
      )
  )

(defn update-user-experience
  "docstring"
  [user experience]
  (if (db-service/update-user-experience (db-service/get-user-by-id (:id user)) experience)
    (let [saved-user (db-service/get-user-by-id (:id user))]
      (if (ready-for-promotion? saved-user)
        {:signal "SUCCESS" :message "You are promoted!." :user (dto-mapper/to-user-dto (promote-user saved-user (get-rank-for-experience (:experience saved-user))))}
        {:signal "SUCCESS" :message "Your new experience is saved." :user (dto-mapper/to-user-dto saved-user)}
        )
      )
    {:signal "FAIL" :message "Server error. User is not updated."}
    )
  )

(defn get-user-by-id
  "docstring"
  [id]
  (dto-mapper/to-user-dto (db-service/get-user-by-id id))
  )
