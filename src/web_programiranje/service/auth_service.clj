(ns web-programiranje.service.auth-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]
            [bcrypt-clj.auth :as bcrypt_encoder]))


(defn get-user-by-username
  "docstring"
  [username]
  (dto-mapper/to-user-dto (db-service/get-user-by-username username))
  )

(defn login-failed-username []
  {:signal "LOGIN_FAILED_USERNAME"}
  )

(defn login-success [user]
  {:signal "LOGIN_SUCCESS" :user (dto-mapper/to-user-dto user)}
  )

(defn login-failed-password []
  {:signal "LOGIN_FAILED_PASSWORD"}
  )

(defn login [login-request]
  (let [user (db-service/get-user-by-username (:username login-request))]
    (println (:password login-request))
    (println (:password user))
    (if (= user nil)
      (login-failed-username)
      (let [encoded-password (subs (:password user) 8)]      ;subs because in database password has perfix {bcrypt}
        (if (bcrypt_encoder/check-password (:password login-request) encoded-password)
          (login-success user)
          (login-failed-password)
          )
        )
      )
    )
  )

(defn registration-failure [message]
  {:signal "FALIURE", :message message}
  )

(defn registration-success [saved-user]
  {:signal "SUCCESS" :message "User is saved." :user (dto-mapper/to-user-dto saved-user)}
  )

(defn register-user [user]
  (if (nil? (db-service/get-user-by-username (:username user)))
    (if (nil? (db-service/get-user-by-email (:email user)))
      (let [saved-user (db-service/insert-user
                         (assoc user :password (str "{bcrypt}" (bcrypt_encoder/crypt-password (:password user)) ))
                         )]
        (if (nil? (:id saved-user))
          (registration-failure "User is not saved")
          (registration-success saved-user)
          )
        )
      (registration-failure (str "Email " (:email user) " is taken"))
      )
    (registration-failure (str "Username " (:username user) " is taken"))
    )
  )