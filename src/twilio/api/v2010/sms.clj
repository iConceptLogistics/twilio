(ns twilio.api.v2010.sms
  "Wraps Twilio's Programmable Messaging API (2010-04-01 Messages).

  Message maps mirror Twilio's actual form-parameter names (PascalCase,
  e.g. `:To`, `:From`, `:Body`). Schemas are permissive (`:closed false`)
  so any extra Twilio-supported fields pass through to the API untouched
  (e.g. `:StatusCallback`, `:MediaUrl`, `:SendAt`)."
  (:refer-clojure :exclude [send])
  (:require [twilio.core :as core]))

(def default-base-url "https://api.twilio.com")

(def CfgSchema
  "twilio.core/CfgSchema plus the Account SID for the URL path."
  [:map {:closed false}
   [:account-sid :string]
   [:api-key     :string]
   [:api-secret  :string]
   [:base-url {:optional true} :string]])

(def SendSchema
  [:and
   [:map {:closed false}
    [:To   :string]
    [:Body :string]]
   [:fn {:error/message "must contain :From or :MessagingServiceSid"}
    (fn [m] (boolean (or (:From m) (:MessagingServiceSid m))))]])

(defn send
  "Send an SMS message.

  `cfg` - {:account-sid :api-key :api-secret & {:base-url :http-opts}}
  `msg` mirrors Twilio's form parameters:
    {:To   \"+15555550100\"
     :From \"+15555550199\"        ; or :MessagingServiceSid
     :Body \"Hello from Clojure\"}

  Any other Twilio fields (:StatusCallback, :MediaUrl, :SendAt, etc.)
  pass through unchanged."
  [cfg msg]
  (core/validate! CfgSchema cfg)
  (core/validate! SendSchema msg)
  (let [{:keys [account-sid api-key api-secret base-url]
         :or   {base-url default-base-url}} cfg]
    (core/request cfg :post
                  (str base-url "/2010-04-01/Accounts/" account-sid "/Messages.json")
                  {:basic-auth  [api-key api-secret]
                   :form-params msg})))
