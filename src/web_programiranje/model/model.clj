(ns web-programiranje.model.model)

(defrecord creature [id name diet-type power-stats])

(defrecord power-stat [id stat amount])

(defrecord stat [id name stat-type])

(defrecord stat-type [id name])

(defrecord game [id name external-info description creator strategies players])

(defrecord player [id name payoffs playable-strategies])

(defrecord payoff [id amount played-strategy opposing-strategy])

(defrecord strategy [id name])

(defrecord user [id first-name last-name username password country date-of-birth is-account-non-locked is-credentials-non-expired is-enabled is-account-non-expired])


(def attack (->stat-type 1 "ATTACK"))
(def slash (->stat 1 "Slash" attack))
(def power-stat-slash (->power-stat 1 slash 44.0))
(def power-stat-slash2 (->power-stat 2 slash 55.0))

(def manticore (->creature 1 "Manticore" "omnivore" (cons power-stat-slash2
                                                                (cons power-stat-slash '()))))