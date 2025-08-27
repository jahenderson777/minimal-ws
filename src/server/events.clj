(ns server.events)

(defmulti handle-event first)

(defmethod handle-event :ping [[_]] [[:assoc-in [:pong] (str (java.time.Instant/now))]])

(defmethod handle-event :echo [[_ payload]] [[:assoc-in [:echo] payload]])

(defmethod handle-event :default [v] [[:assoc-in [:error] {:msg "Unknown event" :event v}]])