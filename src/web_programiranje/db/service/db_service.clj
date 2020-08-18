(ns web-programiranje.db.service.db-service
  (:require [toucan.db :as db]
            [toucan.hydrate :as hydr]
            [web-programiranje.db.model.db-model :as model]
            [web-programiranje.db.config.db-config :as db-config]))
;includes applying database configuration!!!!!!!!!
(db-config/inital-configurantion)

(defn all-creatures [] (model/Creature))
(defn power-stats [] (model/PowerStat))

(defn all-users [] (model/User))

(defn all-games [] (model/Game))

(defn all-strategies [] (model/Strategy))

(defn get-user-by-id [id] (model/User id))

(defn get-user-by-username [username]
  (first (db/select model/User :username username))
  )


(defn- get-players-by-game-id [game_id]
  (map
    #(assoc
       %
       :payoffs
       (hydr/hydrate (db/select model/Payoff :player_id (:id %)) :strategy :opposing_strategy)
       )
    (db/select model/Player :game_id game_id)
    )
  )


(defn get-game-by-id [id]
  (->(hydr/hydrate (model/Game id) :user)
     (assoc :strategies (db/select model/Strategy :game_id id))
     (assoc :players (get-players-by-game-id id))
     )
  )

(defn get-all-games []
  (hydr/hydrate (model/Game) :user)
  )
