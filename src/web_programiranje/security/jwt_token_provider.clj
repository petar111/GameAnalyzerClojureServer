(ns web-programiranje.security.jwt-token-provider
  (:require
    [clj-jwt.core :refer :all]
    [clj-jwt.key :refer [private-key]]
    [clj-time.core :refer [now plus days]]
    [web-programiranje.db.service.db-service :as db-service])
  (:import (java.util Date)))
(def secret-key "1223214124124231452434414!!!dasdww111111")
(def encryption-algorithm :HS512)

(def claim
  {:iss "game-analyzer-clojure-server"
   :exp (plus (now) (days 1))
   :iat (now)})

(defn generate-jwt-token [final-claim]
  (-> final-claim jwt (sign encryption-algorithm secret-key) to-str)
  )


(defn validate-jwt-token
  "docstring"
  [token]
  (if (nil? token)
    (throw (Exception. "Authentication failed. Token is missing."))
    (let [decoded-token (-> (subs token 7) str->jwt :claims)]
      (println (new Date (* 1000 (:exp decoded-token))))
      (if (.before (new Date (* 1000 (:exp decoded-token))) (Date.))
        (throw (Exception. "Authentication failed. Token is expired"))
        (if (nil? (db-service/get-user-by-username (:sub decoded-token)))
          (throw (Exception. "Authentication failed. Username in token is not valid."))
          {:signal "OK"}
          )
        )
      )
    )
  )

; TODO when token is corrupted JSON  exception