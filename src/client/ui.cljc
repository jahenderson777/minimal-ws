(ns client.ui)

(defn ui [state]
  [:div.p10
   [:button
    {:on {:click [[:assoc-in [:clicks] (inc (:clicks state 0))]]}}
    "Click me"]
   [:button
    {:on {:click [[:ws [:echo "hello3!"]]]}}
    "Click me server"]
   [:pre (pr-str state)]])