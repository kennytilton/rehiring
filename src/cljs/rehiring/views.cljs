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
  [
   "Click any job header to show or hide the full listing."
   "Double-click job description to open listing on HN in new tab."
   "All filters are ANDed except as you direct within RegExp fields."
   "Your edits are kept in local storage, so stick to one browser."
   "Works off page scrapes taken every two hours. E-mail <a href='mailto:kentilton@gmail.com'>Kenny</a> if they seem stopped."
   "RFEs welcome and can be raised <a href='https://github.com/kennytilton/whoshiring/issues'>here</a>. "
   "Built with <a href='https://github.com/kennytilton/matrix/blob/master/js/matrix/readme.md'>Matrix Inside</a>&trade;."
   "This page is not affiliated with Hacker News, but..."
   "..thanks to the HN crew for their assistance. All screw-ups remain " +
   "<a href='https://news.ycombinator.com/user?id=kennytilton'>kennytilton</a>'s."
   "Graphic design by <a href='https://www.mloboscoart.com'>Michael Lobosco</a>."
   ])





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
      [:div
       [:header
        [:div {:class    "about"
               :title    "Usage hints, and credit where due."
               :on-click #(do (println :bam2)
                              (reset! helping (not @helping)))}
         "Pro Tips"]
        [:div {:class "headermain"}
         [:span {:class "askhn"} "Ask HN:"]
         [:span {:class "who"} "Who Is Hiring?"]]]
       [help-list appHelpEntry "appHelp" helping]])))

(defn help-list [helpItems helpName helping]
  (fn []
    [:div {:class   (str "help " (utl/slide-in-anime @helping))
           :style   {:display (if @helping "block" "none")}
           :on-click (fn [mx]
                      (reset! helping false))}
     [:div {:style {:cursor      "pointer"
                    :textAlign   "left"
                    :marginRight "18px"}}
      [:ul {:style {:listStyle  "none"
                    :marginLeft 0}}
       (map (fn [ex e]
              [:li {:key   (str helpName "-" ex)
                    :style {:padding 0
                            :margin "0 18px 9px 0"}} e])
         (range)
         helpItems)]]]))

;function helpOff( mx, toggleName, tag='anon') {
;                                               clg('helpoff doing', toggleName, tag)
;                                               mx.fmUp(toggleName).onOff = false
;                                               }
;
;function helpList ( helpItems, toggleName) {
;                                            return div( {
;                                                         class: cF( c=> "help " + slideInRule(c, c.md.fmUp( toggleName).onOff))
;, style: cF( c=> "display:" + (c.md.fmUp(toggleName).onOff? "block":"none"))
;                                                                , onclick: mx => helpOff(mx, toggleName, 'outerdiv')
;                                                         }
;,div({style: "cursor:pointer;text-align: right;margin-right:18px;"
;             , onclick: mx => helpOff(mx, toggleName, 'Xchar')}, "X")
;, ul({ style: "list-style:none; margin-left:0"}
;, helpItems.map( e=> li({style: "padding:0px;margin: 0 18px 9px 0;"}, e)))
;
;                                                        )
;                                            }

;function appBanner () {
;                       return div(
;                                   header(
;                                           div( {
;                                                 class: "about"
;                                                        , onclick: mx=> mx.onOff = !mx.onOff
;    , title: "Usage hints, and credit where due."
;    , content: cF( c=> c.md.onOff? "hide" : "Pro tips")
;                                                 }
;    , {
;       name: "appHelpToggle"
;             , onOff: cI( false)
;       })
;    , div( { class: "headermain"}
;    , span( {class: "askhn"}, "Ask HN:")
;    , span( {class: "who"}, "Who&rsquo;s Hiring?")))
;  , helpList(appHelpEntry,"appHelpToggle"))
;                       }