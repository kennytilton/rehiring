(ns rehiring.views
  (:require
    [reagent.core :as rgt]
    [re-frame.core :as rfr]
    [rehiring.utility :as utl]
    [rehiring.subs :as subs]
    [rehiring.utility :as utl]
    [rehiring.job-loader :as jld]
    [rehiring.job-list :as jls]
    [rehiring.control-panel :as cp]
    ))

(declare help-list app-banner)

(def appHelpEntry
  (map identity
    [
     "Click any job header to show or hide the full listing."
     "Double-click job description to open listing on HN in new tab."
     "All filters are ANDed except as you direct within RegExp fields."
     "Your edits are kept in local storage, so stick to one browser."
     "Works off page scrapes taken often enough. E-mail <a href='mailto:kentilton@gmail.com'>Kenny</a> if they seem stopped."
     "RFEs welcome and can be raised <a href='https://github.com/kennytilton/whoshiring/issues'>here</a>. "
     "Built with <a href='https://github.com/kennytilton/matrix/blob/master/js/matrix/readme.md'>Matrix Inside</a>&trade;."
     "This page is not affiliated with Hacker News, but..."
     "..thanks to the HN crew for their assistance. All screw-ups remain <a href='https://news.ycombinator.com/user?id=kennytilton'>kennytilton</a>'s."
     "Graphic design by <a href='https://www.mloboscoart.com'>Michael Lobosco</a>."
     ]))

(defn main-panel []
  [:div
   [app-banner]
   [:div {:style {:margin 0 :background "#ffb57d"}}
    [jld/pick-a-month]
    [jld/job-listing-loader]
    [cp/control-panel]
    [jls/job-list]]])

(defn app-banner []
  (let [helping (rgt/atom false)]
    (fn []
      [:div {:style {:background "PAPAYAWHIP"}}
       [:header
        [:div.about {
               :title    "Usage hints, and credit where due."
               :on-click #(reset! helping (not @helping))}
         "Pro Tips"]
        [:div.headermain
         [:span.askhn "Ask HN:"]
         [:span.who "Who Is Hiring?"]]]
       [help-list appHelpEntry "appHelp" helping]])))

(defn help-list [helpItems helpName helping]
  (fn []
    [:div {:class    (str "help " (utl/slide-in-anime @helping))
           :style    {:display (if @helping "block" "none")
                      :margin-top 0}
           :on-click (fn [mx]
                       (reset! helping false))}
     [:div {:style {:cursor      "pointer"
                    :textAlign   "left"
                    :marginRight "18px"}}
      [:ul {:style {:listStyle  "none"
                    :marginLeft 0}}
       (map (fn [ex e]
              ^{:key ex} [:li {
                               :style {:padding 0
                                       :margin  "0 18px 9px 0"}}
                          [:div {:dangerouslySetInnerHTML {:__html e}}]])
         (range)
         helpItems)]]]))