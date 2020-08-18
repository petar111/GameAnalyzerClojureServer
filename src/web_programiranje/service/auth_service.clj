(ns web-programiranje.service.auth-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]))


(defn get-user-by-username
  "docstring"
  [username]
  (dto-mapper/to-user-dto (db-service/get-user-by-username username))
  )

(defn login-failed-username []
  {:message "Login failed. Check your username"}
  )

(defn login-success []
  {:message "Login was successful."}
  )

(defn login-failed-password []
  {:message "Login failed. Check your password"}
  )

(defn login [login-request]
  (let [user (first (get-user-by-username (:username login-request)))]
    (if (= user nil)
      (login-failed-username)
      (if (= (:password user) (:password login-request))
        (login-success)
        (login-failed-password)
        )
      )
    )
  )