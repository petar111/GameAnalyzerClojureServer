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
    (if (= user nil)
      (throw (Exception. (str "Username " (:user login-request) " does not exist")))
      (let [encoded-password (subs (:password user) 8)]      ;subs because in database password has perfix {bcrypt}
        (if (bcrypt_encoder/check-password (:password login-request) encoded-password)
          (login-success user)
          (throw (Exception. (str "Password is incorrect.")))
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

(defn register-user
  "docstring"
  [user]
  (if (nil? (db-service/get-user-by-username (:username user)))
    (if (nil? (db-service/get-user-by-email (:email user)))
      (let [saved-user (db-service/insert-user
                         (assoc user :password (str "{bcrypt}" (bcrypt_encoder/crypt-password (:password user)) ))
                         )]
        (if (nil? (:id saved-user))
          (throw (Exception. "User is not saved."))
          (registration-success (assoc saved-user :rank (db-service/get-rank-by-id 1)))
          )
        )
      (throw (Exception. (str "Email " (:email user) " is taken")))
      )
    (throw (Exception. (str "Username " (:username user) " is taken")))
    )
  )

(defn update-response [signal message]
  {:signal signal :message message}
  )

(defn update-user
  "docstring"
  [user]
  (if (or (nil? (db-service/get-user-by-username (:username user))) (= (:id (db-service/get-user-by-username (:username user))) (:id user)))
    (if (or (nil? (db-service/get-user-by-email (:email user))) (= (:id (db-service/get-user-by-email (:email user))) (:id user)))
      (if (db-service/update-user user)
        (if (nil? (:password user))
          (dto-mapper/to-user-dto (db-service/get-user-by-id (:id user)))
            (if (db-service/update-user-password user (str "{bcrypt}" (bcrypt_encoder/crypt-password (:password user))))
              (dto-mapper/to-user-dto (db-service/get-user-by-id (:id user)))
              (throw (Exception. (str "User password is not saved.")))
              )
          )
        (throw (Exception. (str "User is not saved.")))
        )
      (throw (Exception. (str "Email " (:email user) " is taken")))
      )
    (throw (Exception. "Username is taken"))
    ;(update-response "FAIL" (str "Username " (:username user) " is taken") )
    )
  )
