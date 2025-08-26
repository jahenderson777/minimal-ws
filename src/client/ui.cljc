(ns client.ui)

(defn ui [state]
  [:div.p10
   [:h1 "this is an H1"]
   [:h2 "this is an H2"]
   [:h3 "this is an H3"]
   [:h4 "this is an H4"]
   [:p "body text"]
   [:p "another para text"]


   [:button
    {:on {:click [[:assoc-in [:clicks] (inc (:clicks state 0))]]}}
    "Click me"]
   [:button
    {:on {:click [[:ws [:echo "hello3!"]]]}}
    "Click me server"]
   [:pre (pr-str state)]])