(ns server.events)

(defmulti handle-event first)

(defmethod handle-event :ping [[_]] {:pong (str (java.time.Instant/now))})

(defmethod handle-event :echo [[_ payload]] {:echo payload})

(defmethod handle-event :default [v] {:error {:msg "Unknown event" :event v}})