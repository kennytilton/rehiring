(ns rehiring.filtering
  (:require [rehiring.utility :as utl]
            [rehiring.events :as evt]
            [re-frame.core :refer [subscribe reg-sub] :as rfr]
            [re-frame.core :as re-frame]))

(rfr/reg-sub :filter-active-all
  (fn [db [_]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (:filter-active db)))

(rfr/reg-sub :filter-active
  (fn [db [_ tag]]
    (get-in db [:filter-active tag])))

(defn rgx-tree-match [text tree]
  (println :rgxtreematch (subs text 0 20) tree)
  (some (fn [ands]
          (and ands
               (every? (fn [andx]
                         (and andx
                           (boolean (re-find and text))))
                 ands))) tree))

(rfr/reg-sub :jobs-filtered
  ;; signal fn
  (fn [query-v _]
    [(subscribe [:jobs])
     (subscribe [:user-notes])
     (subscribe [:filter-active-all])
     (subscribe [:rgx-tree :title])
     ;(subscribe [:rgx-tree :full])
     ])

  ;; compute
  (fn [[jobs user-notes filters title-rgx-tree]]            ;;  title-rgx-tree full-rgx-tree]]
    (println :filtering!!!!!!!!!! filters (nil? title-rgx-tree))
    (filter (fn [j]
              (let [unotes (get user-notes (:hn-id j))]
                (and (or (not (get filters "REMOTE")) (:remote j))
                     (or (not (get filters "ONSITE")) (:onsite j))
                     (or (not (get filters "INTERNS")) (:interns j))
                     (or (not (get filters "VISA")) (:visa j))
                     (or (not (get filters "Excluded")) (:excluded unotes))
                     (or (not (get filters "Noted")) (pos? (count (:notes unotes))))
                     (or (not (get filters "Applied")) (:applied unotes))
                     (or (not title-rgx-tree) (rgx-tree-match (:title-search j) title-rgx-tree))
                     ;;(or (not full-rgx-tree) (rgx-tree-match (:body-search j) full-rgx-tree))
                     )))
      jobs)))

(rfr/reg-sub :jobs-filtered-excluded
  ;
  ; return excluded jobs regardless of the "excluded" filter, which *selects* those to be shown
  ;
  ;; signal fn
  (fn [query-v _]
    [(subscribe [:jobs-filtered])
     (subscribe [:user-notes])])

  ;; compute
  (fn [[jobs-filtered user-notes]]
    (println :jfilex-sees (count jobs-filtered) (count user-notes))
    (filter (fn [j]
              (get-in user-notes [(:hn-id j) :excluded]))
      jobs-filtered)))

(declare mk-job-selects)

(def title-selects [[["REMOTE", "Does regex search of title for remote jobs"]
                     ["ONSITE", "Does regex search of title for on-site jobs"]]
                    [["INTERNS", "Does regex search of title for internships"]
                     ["VISA", "Does regex search of title for Visa sponsors"]]])

(def user-selects [[["Starred", "Show only jobs you have rated with stars"]
                    ["Noted", "Show only jobs on which you have made a note"]]
                   [["Applied", "Show only jobs you have marked as applied to"]
                    ["Excluded", "Show jobs you exluded from view"]]])

(defn mk-title-selects []
  (mk-job-selects "title" "Title selects" title-selects {}))

(defn mk-user-selects []
  (mk-job-selects "user" "User selects" user-selects {}))

(defn mk-job-selects [key lbl j-major-selects styling]
  (let [f-style (merge utl/hz-flex-wrap {:margin "8px 0 8px 24px"} styling)]
    ^{:key key} [:div {:style f-style}
                 (map (fn [xm j-selects]
                        ^{:key (str key xm)}
                        [:div {:style {:display "flex"
                                       :flex    "no-wrap"}}
                         (map (fn [[tag desc]]
                                ^{:key tag}
                                [:div {:style {:color       "white"
                                               :min-width   "96px"
                                               :display     "flex"
                                               :flex        ""
                                               :align-items "center"}}
                                 [:input {:id        (str tag "ID")
                                          :class     (str tag "-jSelect")
                                          :style     {:background "#eee"}
                                          :type      "checkbox"
                                          :on-change (fn [e]
                                                       (rfr/dispatch [:filter-activate tag (.-checked (.-target e))]))}]
                                 [:label {:for   (str tag "ID")
                                          :title desc}
                                  tag]])
                           j-selects)])
                   (range)
                   j-major-selects)]))



