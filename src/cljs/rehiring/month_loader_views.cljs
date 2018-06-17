(ns rehiring.month-loader-views
  (:require [rehiring.month-loader :as loader]
            [rehiring.utility :as utl]
            [re-frame.core
             :refer [subscribe reg-sub dispatch reg-event-db reg-event-fx]
             :as rfr]
            [cljs.pprint :as pp]))


;;; --- UI: month selecting by user ------------------------------------------

(defn month-load-progress-bar []
  (fn []
    (let [phase @(subscribe [:month-phase])
          max @(subscribe [:month-progress-max])
          made @(subscribe [:month-progress-made])
          hide-me @(subscribe [:month-load-complete?])]
      (println :mk-pg-bar phase made max hide-me)
      [:div {:hidden hide-me}
       [:span (case phase
                :cull-athings "Scrape nodes "
                :parse-jobs "Parse jobs "
                "")]
       [:progress#pgloadprogress
        {:value made
         :max   max}]])))

(defn month-selector []
  (fn []
    (let [months (loader/gMonthlies-cljs)
          mo-id (or @(subscribe [:month-hn-id]) "")]
      [:select {:class        "searchMonth"
                :defaultValue mo-id
                :on-change    #(do (println :bam-mo (.-value (.-target %)))
                                   (dispatch [:month-set (.-value (.-target %))]))}
       (let []
         (map (fn [mno mo-def]
                (let [{:keys [hnId desc] :as all} mo-def]
                  ^{:key mno} [:option {:key hnId :value hnId} desc]))
           (range)
           months))])))

(defn hn-month-link []
  (fn []
    [utl/view-on-hn {}                                      ;; {:hidden (nil? @(subscribe [:month-hn-id]))}
     (pp/cl-format nil "https://news.ycombinator.com/item?id=~a" @(subscribe [:month-hn-id]))]))

(defn month-jobs-total []
  (fn []
    [:span {:style  {:color  "#fcfcfc"
                     :margin "0 12px 0 12px"}
            :hidden (not @(subscribe [:month-load-complete?]))}
     (str "Total jobs: " (count @(subscribe [:month-jobs])))]))

;;; --- the beef ------------------------------------------------------------

(defn pick-a-month []
  [:div {:class "pickAMonth"}
   [month-selector]

   [:div {:style utl/hz-flex-wrap}
    [hn-month-link]
    [month-jobs-total]
    [month-load-progress-bar]]])



