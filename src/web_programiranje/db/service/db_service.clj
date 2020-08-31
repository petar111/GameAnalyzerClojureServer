(ns web-programiranje.db.service.db-service
  (:require [toucan.db :as db]
            [toucan.hydrate :as hydr]
            [web-programiranje.db.model.db-model :as model]
            [web-programiranje.db.config.db-config :as db-config]
            [bcrypt-clj.auth :as encoder]
            )
  (:import (java.util Date)
           (java.time LocalDateTime ZonedDateTime)
           (java.text SimpleDateFormat)))
;includes applying database configuration!!!!!!!!!
(db-config/inital-configurantion)

(defn all-creatures [] (model/Creature))
(defn power-stats [] (model/PowerStat))

(defn all-users [] (model/User))

(defn all-games [] (model/Game))



(defn all-strategies [] (model/Strategy))

(defn enrich-user
  "docstring"
  [user]
  (assoc user :followers_count (count (db/select model/UserFollowing :user_following_id (:id user)))
              :following_count (count (db/select model/UserFollowing :user_id (:id user))))
  )

(defn get-user-by-id [id] (enrich-user (hydr/hydrate (model/User id) :rank)))

(defn get-user-by-username [username]
  (enrich-user (first (hydr/hydrate (db/select model/User :username username) :rank)))
  )

(defn get-user-by-email [email]
  (enrich-user (first (hydr/hydrate (db/select model/User :email email) :rank)))
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
  (-> (hydr/hydrate (model/Game id) :user :verification_status)
      (assoc :strategies (db/select model/Strategy :game_id id))
      (assoc :players (get-players-by-game-id id))
      )
  )

(defn get-all-games-by-user-id [user_id]
  (hydr/hydrate (db/select model/Game :user_id user_id) :user :verification_status)
  )


(defn get-all-games []
  (hydr/hydrate (model/Game) :user :verification_status)
  )

(defn get-all-games-page [page pageSize]
  (hydr/hydrate (db/select model/Game {:limit (Integer/parseInt pageSize) :offset (* (Integer/parseInt page) (Integer/parseInt pageSize))}) :user :verification_status)
  )

(defn get-all-games-count []
  (first (vals (first (db/query {:select [:%count.id]
                                 :from   [:game]}))))
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
  (db/insert! model/Payoff {:amount               (:amount payoff),
                            :player_id            (:id player),
                            :strategy_id          (:id (first (filter #(= (:name %) (:name (:playedStrategy payoff))) strategies))),
                            :opposing_strategy_id (:id (first (filter #(= (:name %) (:name (:opposingStrategy payoff))) strategies)))
                            })
  )

(defn insert-game [game]
  (let [saved-game (db/insert! model/Game {:name          (:name game),
                                           :description   (:description game),
                                           :external_info (:externalInfo game),
                                           :user_id       (:id (:creator game))})]
    (assoc saved-game :saved-players (let [strategies (insert-strategies (:strategies game) (:id saved-game))
                                           players (insert-players (:players game) (:id saved-game))]
                                       (for [player players]
                                         (assoc player :saved-payoffs (loop [payoff (:payoffs player) result []]
                                                                        (if (seq payoff)
                                                                          (recur (rest payoff) (conj result (insert-payoff (first payoff) player strategies)))
                                                                          result
                                                                          )
                                                                        ))
                                         )
                                       ))
    )
  )

(defn find-game-by-name [name]
  (let [game (first (db/select model/Game :name name))]
    (get-game-by-id (:id game))
    )
  )

(defn- get-player-by-id [id]
  (assoc
    (model/Player id)
    :payoffs
    (hydr/hydrate (db/select model/Payoff :player_id id) :strategy :opposing_strategy)
    )
  )

(defn- get-game-session-players-by-game-session-id [game-session-id]
  (map #(assoc
          % :played_strategies
            (hydr/hydrate (db/select model/GameSessionPlayerStrategy :game_session_player_id (:id %)) :strategy)
            ) (map #(assoc % :player (get-player-by-id (:player_id %))) (hydr/hydrate (db/select model/GameSessionPlayer :game_session_id game-session-id) :selected_strategy))
       )
  )

(defn get-game-session-by-id [id]
  (let [result (hydr/hydrate (model/GameSession id) :user)]
    (assoc
      (assoc result :players (get-game-session-players-by-game-session-id id))
      :game
      (get-game-by-id (:game_id result)))
    )
  )

(defn- insert-game-session-player-strategies [game-session-player-strategies game-session-player-id]
  (loop [strategy game-session-player-strategies result []]
    (if (seq strategy)
      (let [curr-strategy (first strategy)]
        (recur (rest strategy) (conj result (db/insert! model/GameSessionPlayerStrategy {
                                                                                         :times_played           (:timesPlayed curr-strategy),
                                                                                         :game_session_player_id game-session-player-id,
                                                                                         :strategy_id            (:id (:strategy curr-strategy))
                                                                                         })))
        )
      result
      )
    )
  )

(defn- insert-game-session-players [game-session-players game-session-id]
  (loop [players game-session-players result []]
    (if (seq players)
      (let [saved-player (db/insert! model/GameSessionPlayer {:game_session_id      game-session-id,
                                                              :player_id            (:id (:player (first players))),
                                                              :player_label         (:playerLabel (first players)),
                                                              :total_payoff         (:totalPayoff (first players)),
                                                              :selected_strategy_id (:id (:selectedStrategy (first players)))
                                                              })]
        (recur (rest players) (conj result (assoc saved-player :saved_played_strategies (insert-game-session-player-strategies (:playedStrategies (first players)) (:id saved-player)))))
        )
      result
      )
    )
  )

(defn insert-game-session
  "docstring"
  [game-session]
  (let [saved-game-session (db/insert! model/GameSession {
                                                          :number_of_rounds (:numberOfRounds game-session),
                                                          :user_id          (:id (:creator game-session)),
                                                          :game_id          (:id (:game game-session))
                                                          })]
    (assoc saved-game-session :saved_players (insert-game-session-players (:players game-session) (:id saved-game-session)))
    )
  )

(defn- update-game-session-player-strategies [game-session-player-strategies]
  (loop [strategy game-session-player-strategies result []]
    (if (seq strategy)
      (let [curr-strategy (first strategy)]
        (recur (rest strategy) (conj result (db/update! model/GameSessionPlayerStrategy (:id curr-strategy) {
                                                                                                             :times_played (:timesPlayed curr-strategy)
                                                                                                             })))
        )
      (empty? (filter #(= % false) result))
      )
    )
  )

(defn- update-game-session-players [game-session-players]
  (loop [players game-session-players result []]
    (if (seq players)
      (let [saved-player (db/update! model/GameSessionPlayer (:id (first players)) {
                                                                                    :player_label         (:playerLabel (first players)),
                                                                                    :total_payoff         (:totalPayoff (first players)),
                                                                                    :selected_strategy_id (:id (:selectedStrategy (first players)))
                                                                                    })]
        (recur (rest players) (conj result (update-game-session-player-strategies (:playedStrategies (first players)))))
        )
      (empty? (filter #(= % false) result))
      )
    )
  )

(defn update-game-session
  "docstring"
  [game-session]
  (let [saved-game-session (db/update! model/GameSession (:id game-session) {
                                                                             :number_of_rounds (:numberOfRounds game-session)
                                                                             })]
    (and saved-game-session (update-game-session-players (:players game-session)))
    )
  )

(defn get-game-session-by-creator-username [username]
  (hydr/hydrate (db/select model/GameSession :user_id (:id (get-user-by-username username))) :game)
  )

(defn insert-user [user]
  (db/insert! model/User {
                          :first_name                 (:firstName user),
                          :last_name                  (:lastName user),
                          :country                    (:country user),
                          :date_of_birth              (.parse (SimpleDateFormat. "yyyy-MM-dd") (:dateOfBirth user)),
                          :username                   (:username user),
                          :email                      (:email user),
                          :password                   (:password user),
                          :is_account_non_expired     (Boolean. true),
                          :is_account_non_locked      (Boolean. true),
                          :is_enabled                 (Boolean. true),
                          :is_credentials_non_expired (Boolean. true),
                          :experience                 0,
                          :rank_id                    1
                          })
  )

(defn get-user-followers-usernames-by-user-id [user_id]
  (let [followers-ids (map :user_id (db/select [model/UserFollowing :user_id] :user_following_id user_id))]
    (if (seq followers-ids)
      (map :username (db/select [model/User :username] :id [:in followers-ids]))
      '()
      )
    )
  )

(defn get-user-following-usernames-by-user-id [user_id]
  (let [following-ids (map :user_following_id (db/select [model/UserFollowing :user_following_id] :user_id user_id))]
    (if (seq following-ids)
      (map :username (db/select [model/User :username] :id [:in following-ids]))
      '()
      )
    )

  )

(defn update-user [user]
  (db/update! model/User (:id user) {
                                     :first_name                 (:firstName user),
                                     :last_name                  (:lastName user),
                                     :country                    (:country user),
                                     :date_of_birth              (.parse (SimpleDateFormat. "yyyy-MM-dd") (:dateOfBirth user)),
                                     :username                   (:username user),
                                     :email                      (:email user),
                                     :is_account_non_expired     (:isAccountNonExpired user),
                                     :is_account_non_locked      (:isAccountNonLocked user),
                                     :is_enabled                 (:isEnabled user),
                                     :is_credentials_non_expired (:isCredentialsNonExpired user),
                                     })
  )

(defn update-user-password
  "docstring"
  [user crypted_password_with_encoder_prefix]
  (db/update! model/User (:id user) {
                                     :password crypted_password_with_encoder_prefix
                                     })
  )

(defn get-game-score-by-id
  "docstring"
  [id]
  (hydr/hydrate (model/GameScore id) [:game :user] :user)
  )

(defn get-all-game-score-by-game-id [game_id]
  (hydr/hydrate (db/select model/GameScore :game_id game_id) [:game :user] :user)
  )

(defn get-all-game-score-by-user-id [user_id]
  (hydr/hydrate (db/select model/GameScore :user_id user_id) [:game :user] :user)
  )

(defn get-all-game-score-by-user-id-and-game-id [user_id game_id]
  (hydr/hydrate (db/select model/GameScore :user_id user_id :game_id game_id) [:game :verification_status :user] [:user :rank])
  )

(defn insert-game-score [game-score]
  (db/insert! model/GameScore {
                               :total_payoff     (:totalPayoff game-score),
                               :number_of_rounds (:numberOfRounds game-score),
                               :user_id          (:id (:user game-score)),
                               :game_id          (:id (:game game-score)),
                               :date_created     (Date.)
                               })
  )

(defn get-top-game-scores-by-number-of-rounds-and-game-id [number_of_rounds game_id number_of_top]
  (db/select model/GameScore :number_of_rounds number_of_rounds :game_id game_id {:limit number_of_top :order-by [[:total_payoff :desc]]})
  )

(defn update-user-experience [user experience]
  (db/update! model/User (:id user) {
                                     :experience (+ experience (:experience user))
                                     })
  )

(defn get-rank-by-experience [experience]
  (first (db/select model/Rank :experience_max [:>= experience] :experience_min [:<= experience]))
  )

(defn update-user-rank [user_id rank_id]
  (db/update! model/User user_id {
                                  :rank_id rank_id
                                  })
  )

(defn get-rank-by-id
  "docstring"
  [id]
  (model/Rank id)
  )

(defn update-game-verification [game verification-status]
  (db/update! model/Game (:id game) {
                                     :verification_status_id (:id (first (db/select model/VerificationStatus :name verification-status)))
                                     })
  )

(defn update-user-verified-games [creator]
  (db/update! model/User (:id creator) {
                                        :number_of_verified_games (:number_of_verified_games creator)
                                        })
  )

(defn get-user-followers-count-by-user-id [user_id]
  (count (db/select model/UserFollowing :user_following_id user_id))
  )

(defn get-user-following-count-by-user-id [user_id]
  (count (db/select model/UserFollowing :user_id user_id))
  )

(defn get-all-game-scores-by-date-created-and-limit [date amount]
  (hydr/hydrate (db/select model/GameScore :date_created date {:limit amount}) [:game :verification_status :user] [:user :rank] )
  )






;(defn- get-players-by-game-id [game_id]
;  (map
;    #(assoc
;       %
;       :payoffs
;       (hydr/hydrate (db/select model/Payoff :player_id (:id %)) :strategy :opposing_strategy)
;       )
;    (db/select model/Player :game_id game_id)
;    )
;  )
;
;
;(defn get-game-by-id [id]
;  (-> (hydr/hydrate (model/Game id) :user)
;      (assoc :strategies (db/select model/Strategy :game_id id))
;      (assoc :players (get-players-by-game-id id))
;      )
;  )