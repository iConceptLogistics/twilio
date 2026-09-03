(ns send-email
  "Manual smoke test: actually sends an email via Twilio Email.

  Credentials come from the environment so nothing secret lands in the repo:

    export TWILIO_API_KEY=SK...
    export TWILIO_API_SECRET=...
    clojure -M:dev -m send-email noreply@your-domain.com you@example.com

  The :from address must be on a domain you've authenticated in the
  Twilio Console (Products & Services > Email > Domains)."
  (:require [twilio.api.v1.email :as email]))

(defn -main [& [from to]]
  (let [api-key    (System/getenv "TWILIO_API_KEY")
        api-secret (System/getenv "TWILIO_API_SECRET")]
    (when-not (and api-key api-secret from to)
      (println "Usage: TWILIO_API_KEY=SK... TWILIO_API_SECRET=... clojure -M:dev -m send-email <from> <to>")
      (System/exit 1))
    (let [cfg  {:api-key api-key :api-secret api-secret}
          resp (email/send cfg
                           (email/simple-body
                            {:from      from
                             :from-name "twilio-client"
                             :to        to
                             :subject   "twilio-client smoke test"
                             :html      "<p>Hello from <b>twilio-client</b> 🎉</p>"}))]
      (println "Accepted!")
      (println "  operationId:      " (:operationId resp))
      (println "  operationLocation:" (:operationLocation resp)))))
