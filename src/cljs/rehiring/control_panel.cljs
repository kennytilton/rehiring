(ns rehiring.control-panel
  (:require [rehiring.filtering :as flt]
            [rehiring.utility :as utl]
            [rehiring.job-listing-control-bar :as jlcb]
            [re-frame.core :as rfr]))

;function toggleChar ( name, title, initialState, onChar, offChar, attrs={}, locals={},style="") {
;       return span( Object.assign( {
;                                    style: "font-weight:bold; cursor:pointer; margin-left:9px; font-family:Arial; font-size:1em;"+style
;, onclick: mx => mx.onOff = !mx.onOff
;, title: title
;, content: cF( c=> c.md.onOff? onChar:offChar)
;                                                                                                                  }, attrs)
;, Object.assign( {
;      name: name
;            , onOff: cI( initialState)
;      }, locals))

(defn toggle-char [db-key title on-char off-char attrs style]
  (fn []
    (let [on-off @(rfr/subscribe [db-key])]
      [:span (merge {:style    (merge {:font-weight "bold"
                                       :cursor      "pointer"
                                       :margin-left "9px"
                                       :font-family "Arial"
                                       :font-size   "1em"} style)
                     :title    title
                     :on-click #(rfr/dispatch [:toggle-key db-key])}
               attrs)
       (utl/unesc (if on-off on-char off-char))])))

(rfr/reg-sub :show-filters
  (fn [db]
    (:show-filters db)))

(rfr/reg-event-db :toggle-key
  (fn [db [_ db-key]]
    (update db db-key not)))

(defn open-shut-case [toggle-db-key title & cases]
  (fn []
    [:div
     [:div {:class "selector"}
      [:span title]
      [toggle-char toggle-db-key
       (str "Show/hide " title)
       "&#x25be", "&#x25b8" {} {}]]

     [:div {:class "osBody"
            :style {:background "#ff6600"
                    :display    (if @(rfr/subscribe [toggle-db-key]) "block" "none")}}
      (for [case cases]
        ^{:key (rand-int 100000)} (case))]]))

;;; --- sort bar -----------------------------------------------------------

(defn job-company-key [j]
  (or (:company j) ""))

(defn job-stars-compare [dir j k]
  ;; force un-starred to end regardless of sort order
  ;; order ties by ascending hn-id
  (let [j-stars @(rfr/subscribe [:unotes-prop (:hn-id j) :stars])
        k-stars @(rfr/subscribe [:unotes-prop (:hn-id k) :stars])]
    (if (pos? j-stars)
      (if (pos? k-stars)
        (* dir (if (< j-stars k-stars)
                 -1
                 (if (> j-stars k-stars)
                   1
                   (if (< (:hn-d j) (:hn-id k)) -1 1))))
        -1)
      (if (pos? k-stars)
        1
        (if (< (:hn-d j) (:hn-id k)) -1 1)))))

(defn sort-bar []
  (fn []
    [:div {:style {:padding 0
                   :margin  "15px 0 0 24px"
                   :display "flex"}}
     [:span "Sort"]
     [:ul {:style (merge utl/hz-flex-wrap
                    {:list-style "none"
                     :padding 0 :margin 0})}
      (into [] (map (fn [jsort]
             (println :js jsort)
             [:li (:title jsort)]
             #_
                 (let [{:keys [title]} jsort]
                   ;; ^{:key title}
                   [:button.sortOption
                    {:style    {:color (if (= title (:title curr-sort))
                                         "blue" "#222")}
                     :selected (= jsort curr-sort)
                     :on-click #(if (= title (:title curr-sort))
                                  (rfr/dispatch [:job-sort-set (update curr-sort :sort-dir not)])
                                  (rfr/dispatch [:job-sort-set jsort]))}
                    (str (:title jsort) (if (= title (:title curr-sort))
                                          (if (= (:order curr-sort) -1) " &#x2798" " &#x279a")))]))
        utl/job-sorts))
      [:li "one"][:li "two"]]]))

(defn control-panel []
  (fn []
    [:div {:style {:background "#ffb57d"}}
     [open-shut-case :show-filters "Filters"
      flt/mk-title-selects
      flt/mk-user-selects]
     [sort-bar]

     [jlcb/job-listing-control-bar]]))



;function openShutCase( name, title, initOpen, echo, ...cases) {
;                                                               let toggleName = name+"-toggle";
;                                                                   return div(
;                                                                               div({class: "selector"}
;  , span( title)
;  , toggleChar( toggleName, "Show/hide "+title, initOpen, "&#x25be", "&#x25b8")
;  , echo)
;  , div( { class: cF( c=> "osBody " + slideInRule(c, c.md.fmUp(toggleName).onOff))
;  , style: cF( c=> "background:#ff6600;display:" + (c.md.fmUp(toggleName).onOff? "block":"none"))}
;  , cases.map( c=> c())))
;                                                               }