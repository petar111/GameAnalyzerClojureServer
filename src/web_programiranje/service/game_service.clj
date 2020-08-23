(ns web-programiranje.service.game-service
  (:require [web-programiranje.db.service.db-service :as db-service] ;includes applying database configuration
            [web-programiranje.mapper.mapper :as dto-mapper]
            [clojure.math.numeric-tower :as math]))


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
    (if (seq strategy)
      (recur (rest strategy) (concat result (filter #(= (:amount %) (max-amount (filter-player-payoffs-by-opposing-strategy game player-name (first strategy)) )) (filter-player-payoffs-by-opposing-strategy game player-name (first strategy)) ) ))
      result
      )
    )
  )

(defn- get-nash-equilibria [game]
  (loop [dominant-payoff (get-dominant-payoffs-for-player game "Player1" "Player2") result []]
    (if (seq dominant-payoff)
      (recur (rest dominant-payoff) (concat result (filter #(and (= (:id (:playedStrategy %)) (:id (:opposingStrategy (first dominant-payoff)))) (= (:id (:opposingStrategy %)) (:id (:playedStrategy (first dominant-payoff)))) ) (get-dominant-payoffs-for-player game "Player2" "Player1") ) ))
      result
      )
    )
  )

(defn get-game-advice-by-id [id]
  (dto-mapper/to-game-advice-data-dto
    (-> {}
        (assoc :nash-equilbria (get-nash-equilibria (dto-mapper/to-game-dto
                                                         (db-service/get-game-by-id id))))
        )

    )

  )

(defn get-game-score-error []
  {:message "No ids found."}
  )

(defn get-game-scores [user_id game_id]
  (if (nil? user_id)
    (if (nil? game_id)
      (get-game-score-error)
      (dto-mapper/to-game-score-list-dto (db-service/get-all-game-score-by-game-id game_id))
      )
    (if (nil? game_id)
      (dto-mapper/to-game-score-list-dto (db-service/get-all-game-score-by-user-id user_id))
      (dto-mapper/to-game-score-list-dto (db-service/get-all-game-score-by-user-id-and-game-id user_id game_id))
      )
    )
  )



(defn- calculate-players-max-payoff-for-game-by-number-of-rounds [game_id number_of_rounds player_id]
  (let [game (get-game-by-id game_id)]
    (println player_id)
    (let [player (first (filter #(= (:id %) player_id) (:players game)))]
      (* number_of_rounds (apply max (map :amount (:payoffs player))))
      )
    )
  )

(defn- experience-max-payoff [saved-game-score player_id]
  (if (= (:total_payoff saved-game-score) (calculate-players-max-payoff-for-game-by-number-of-rounds (:game_id saved-game-score) (:number_of_rounds saved-game-score) player_id))
    10
    0
    )
  )

(defn- experience-score-in-top-5 [saved-game-score]
  (let [top-payoffs (map :total_payoff (db-service/get-top-game-scores-by-number-of-rounds-and-game-id (:number_of_rounds saved-game-score) (:game_id saved-game-score) 5))]
    (println top-payoffs)
    (case (.indexOf top-payoffs (:total_payoff saved-game-score))
      0 100
      1 50
      2 30
      3 20
      4 10
      0
      )
    )
  )

(defn- calculate-experience [saved-game-score player_id]
  (+ (experience-max-payoff saved-game-score player_id)
     (experience-score-in-top-5 saved-game-score))
  )

(defn insert-game-score [game-score]
  (let [saved-game-score (db-service/insert-game-score game-score)]
    (if (nil? (:id saved-game-score))
      {:signal "FAIL" :message "Game score not saved"}
      {:signal "SUCCESS" :message (str "Game score is saved. You earned " (calculate-experience saved-game-score (:id (:player game-score))) " new experience.") :experience (calculate-experience saved-game-score (:id (:player game-score)))}
      )
    )
  )

(defn attempt-verification [creator game]
  (let [verification-status (cond
                              (< (:number_of_verified_games creator) (:verified_games_max (:rank creator))) "VERIFIED"
                              (>= (:number_of_verified_games creator) (:verified_games_max (:rank creator))) "REJECTED"
                              )]
    {:signal verification-status :updated (db-service/update-game-verification game verification-status)}
    )
  )

(defn- is-game-verified? [game-info]
  (= (:name (:verification_status (db-service/get-game-by-id (:id game-info)))) "VERIFIED")
  )

(defn request-verification
  "docstring"
  [game-info]
  (if (is-game-verified? game-info)
    {:signal "GAME_ALREADY_VERIFIED" :game game-info}
    (let [creator (db-service/get-user-by-username (:creatorUsername game-info))]
      (let [verification-result (attempt-verification creator game-info)]
        (if (not (:updated verification-result))
          (throw (Exception. "Game verification is not updated."))
          (case (:signal verification-result)
            "VERIFIED" (if (db-service/update-user-verified-games (assoc creator :number_of_verified_games (+ 1 (:number_of_verified_games creator))))
                         {:signal "GAME_VERIFIED" :game (dto-mapper/to-game-info-dto (db-service/get-game-by-id (:id game-info)))}
                         (throw (Exception. "User number of verified games is not updated."))
                         )
            "REJECTED" {:signal "GAME_REJECTED" :game (dto-mapper/to-game-info-dto (db-service/get-game-by-id (:id game-info)))}
            )
          )
        )
      )
    )
  )