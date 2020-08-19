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
  (-> (hydr/hydrate (model/Game id) :user)
      (assoc :strategies (db/select model/Strategy :game_id id))
      (assoc :players (get-players-by-game-id id))
      )
  )

(defn get-all-games []
  (hydr/hydrate (model/Game) :user)
  )

(defn execute-transaction
  "docstring"
  [transaction-body]
  (db/transaction
    transaction-body
    )
  )

(defn- insert-strategies [strategies game_id]
  (loop [strategy_loop strategies result []]
    (if (seq strategy_loop)
      (let [saved-strategy (db/insert! model/Strategy {:name (:name (first strategy_loop)), :game_id game_id})]
        (recur (rest strategy_loop) (conj result saved-strategy))
        )
      result
      )
    )
  )

(defn- insert-players [players game_id]
  (loop [player_loop players result []]
    (if (seq player_loop)
      (let [saved-player (db/insert! model/Player {:name (:name (first player_loop)), :game_id game_id})]
        (recur (rest player_loop) (conj result (assoc saved-player :payoffs (:payoffs (first player_loop)))))
        )
      result
      )
    )
  )

(defn- insert-payoff [payoff player strategies]
  (db/insert! model/Payoff {:amount (:amount payoff),
                            :player_id (:id player),
                            :strategy_id (:id (first (filter #(= (:name %) (:name (:playedStrategy payoff))) strategies))),
                            :opposing_strategy_id (:id (first (filter #(= (:name %) (:name (:opposingStrategy payoff))) strategies)))
                            })
  )

(defn insert-game [game]
  (let [saved-game (db/insert! model/Game {:name          (:name game),
                                           :description   (:description game),
                                           :external_info (:externalInfo game),
                                           :user_id       (:id (:creator game))})]
    (let [strategies (insert-strategies (:strategies game) (:id saved-game))
          players (insert-players (:players game) (:id saved-game))]
      (for [player players]
        (loop [payoff (:payoffs player)]
          (when (seq payoff)
            (insert-payoff (first payoff) player strategies)
            (recur (rest payoff))
            )
          )
      )
      )
  )
  )
