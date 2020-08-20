(ns web-programiranje.service.game-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]))


(defn get-all-games
  "docstring"
  []
  (dto-mapper/to-game-info-list-dto
    (db-service/get-all-games))
  )

(defn get-game-by-id
  "docstring"
  [id]
  (dto-mapper/to-game-dto
    (db-service/get-game-by-id id))
  )

(defn insert [game]
  (let [saved-game (db-service/execute-transaction (db-service/insert-game game))]
    (if (empty? (:saved-players saved-game))
      {:message (str "Game is NOT saved")}
      {:message (str "Game " (:name saved-game) " is saved")}
      )
    )
  )

(defn get-by-name [name]
  (dto-mapper/to-game-dto (db-service/find-game-by-name name))
  )

(defn get-game-session-by-id [id]
  (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id id))
  )

(defn insert-game-session [game-session]
  (let [saved-game-session (db-service/execute-transaction (db-service/insert-game-session game-session))]
    (if (empty? (:saved_players saved-game-session))
      {:message (str "Error. Game session not saved")}
      (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id (:id saved-game-session)))
      )
    )
  )

(defn update-game-session [game-session]
  (let [saved-game-session (db-service/execute-transaction (db-service/update-game-session game-session))]
    (if saved-game-session
      (dto-mapper/to-game-session-dto (db-service/get-game-session-by-id (:id game-session)))
      {:message (str "Error. Game session not saved")}
      )
    )
  )

(defn save-game-session [game-session]
  (if (nil? (:id game-session))
    (insert-game-session game-session)
    (update-game-session game-session)
    )
  )

(defn get-game-session-infos-by-creator-username [username]
  (println username)
  (dto-mapper/to-game-session-info-list-dto (db-service/get-game-session-by-creator-username username))
  )

(defn- max-amount [payoffs]
  (apply max (loop [map payoffs result [] ]
               (if (seq map)
                 (recur (rest map) (conj result (:amount (first map))))
                 result
                 )
               ))
  )

(defn- filter-player-payoffs-by-opposing-strategy
  "docstring"
  [game player-name opposing-strategy]
  (filter #(= (:id (:opposingStrategy %)) (:id opposing-strategy) ) (:payoffs (first (filter #(= (:name %) player-name) (:players game)))) )
  )

(defn- get-dominant-payoffs-for-player [game player-name opp-player-name]
  (loop [strategy (:playableStrategies (first (filter #(= (:name %) opp-player-name) (:players game)))) result []]
    (println (filter-player-payoffs-by-opposing-strategy game player-name (first strategy))  )
    (if (seq strategy)
      (recur (rest strategy) (concat result (filter #(= (:amount %) (max-amount (filter-player-payoffs-by-opposing-strategy game player-name (first strategy)) )) (filter-player-payoffs-by-opposing-strategy game player-name (first strategy)) ) ))
      result
      )
    )
  )

(defn- get-nash-equilibria [game]
  (loop [dominant-payoff (get-dominant-payoffs-for-player game "Player1" "Player2") result []]
    (println (first dominant-payoff))
    (if (seq dominant-payoff)
      (recur (rest dominant-payoff) (concat result (filter #(and (= (:id (:playedStrategy %)) (:id (:opposingStrategy (first dominant-payoff)))) (= (:id (:opposingStrategy %)) (:id (:playedStrategy (first dominant-payoff)))) ) (get-dominant-payoffs-for-player game "Player2" "Player1") ) ))
      result
      )
    )
  )

(defn get-game-advice-by-id [id]
  (get-nash-equilibria (dto-mapper/to-payoff-list-dto
                          (db-service/get-game-by-id id)))
  )