(ns rehiring.filtering
  (:require [rehiring.utility :as utl]
            [rehiring.events :as evt]
            [re-frame.core :as rfr]
            [re-frame.core :as re-frame]))

(defn job-list-filter [jobs]
  (if (empty? jobs)
    []
    (let [remall @(rfr/subscribe [:filter-active-all])]
      (filter (fn [j]
                (let [unotes @(rfr/subscribe [:unotes (:hn-id j)])]
                  (and (or (not (get remall "REMOTE")) (:remote j))
                       (or (not (get remall "ONSITE")) (:onsite j))
                       (or (not (get remall "INTERNS")) (:interns j))
                       (or (not (get remall "VISA")) (:visa j))
                       (or (not (get remall "Excluded")) (:excluded unotes))
                       (or (not (get remall "Noted")) (pos? (count (:notes unotes))))
                       (or (not (get remall "Starred")) (pos? (:stars unotes)))
                       (or (not (get remall "Applied")) (:applied unotes)))))
        jobs))))

(rfr/reg-sub :filter-active-all
  (fn [db [_]]
    ;;(println :sub-runs! hn-id (get-in db [:show-job-details hn-id]))
    (:filter-active db)))

(rfr/reg-sub :filter-active
  (fn [db [_ tag]]
    (get-in db [:filter-active tag])))

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
                                           (println :bamchg
                                             (.-checked (.-target e)))
                                           (rfr/dispatch [:filter-activate tag (.-checked (.-target e))]))}]
                     [:label {:for   (str tag "ID")
                              :title desc}
                      tag]])
               j-selects)])
       (range)
       j-major-selects)]))

;;; --- events -------------------------------------------------------

(rfr/reg-event-db
  :filter-activate
  (fn [db [_ tag active?]]
    (println :bamevt tag active? :now (get-in db [:filter-active tag]))
    (assoc-in db [:filter-active tag] active?)))

