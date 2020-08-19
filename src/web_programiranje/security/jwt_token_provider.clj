(ns web-programiranje.security.jwt-token-provider
  (:require
    [clj-jwt.core  :refer :all]
    [clj-jwt.key   :refer [private-key]]
    [clj-time.core :refer [now plus days]]))
(def secret-key "1223214124124231452434414!!!dasdww111111")

(def claim
  {:iss "game-analyzer-clojure-server"
   :exp (plus (now) (days 1))
   :iat (now)})

(defn generate-jwt-token [final-claim]
  (-> final-claim jwt (sign :HS512 secret-key) to-str)
  )

