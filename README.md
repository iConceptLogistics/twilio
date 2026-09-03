# twilio-client

A minimal Clojure wrapper around [Twilio](https://www.twilio.com/)'s SMS
(Programmable Messaging) and [Twilio Email](https://www.twilio.com/docs/email)
APIs.

- Namespaces mirror the API version (`twilio.api.v2010.sms`, `twilio.api.v1.email`).
- Every public function takes an explicit `cfg` map as its first argument; no global state.
- Inputs are validated up-front with [malli](https://github.com/metosin/malli).
  Schemas are permissive: required fields are checked, but extra Twilio-supported
  fields pass through to the API untouched.
- HTTP errors surface as `ex-info` with `:status`, `:body`, and `:url`.

## Installation

`deps.edn`:

```clojure
{:deps {com.example/twilio-client {:git/url "https://github.com/your-org/twilio-client"
                                   :git/sha "..."}}}
```

## Configuration

Both products authenticate with a Twilio API Key SID/Secret pair (Basic
auth). Create one in the Twilio Console under **Settings > Account
settings > API keys & auth tokens**; the secret is shown only once, at
creation. SMS additionally needs your Account SID for the URL path, so
one map covers both:

```clojure
(def cfg
  {:account-sid "ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" ; SMS only
   :api-key     "SKXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
   :api-secret  "your_api_key_secret"})
```

Optional keys: `:base-url` to override the API host, `:http-opts` to
override clj-http defaults (e.g. timeouts).

## SMS

```clojure
(require '[twilio.api.v2010.sms :as sms])

(sms/send cfg
  {:To   "+15555550100"
   :From "+15555550199"          ;; or :MessagingServiceSid "MG..."
   :Body "Hello from Clojure"})
;; => {:sid "SM..." :status "queued" ...}
```

Message maps use Twilio's actual form-parameter names (PascalCase). The
schema requires `:To`, `:Body`, and one of `:From`/`:MessagingServiceSid`;
any other Twilio fields (`:StatusCallback`, `:MediaUrl`, `:SendAt`, etc.)
pass through unchanged.

## Email

Twilio Email requires an authenticated sending domain (Console:
**Products & Services > Email > Domains**); any address on a verified
domain may be used as `:from`.

```clojure
(require '[twilio.api.v1.email :as email])

(email/send cfg
  {:from    {:address "noreply@example.com" :name "Example"}
   :to      [{:address "user@example.com"}]
   :content {:subject "Welcome"
             :html    "<p>Hi there</p>"}})
;; => {:operationId "comms_operation_..." :operationLocation "https://comms.twilio.com/v1/Emails/Operations/..."}
```

Sends are asynchronous: Twilio responds `202 Accepted`, and a `GET` on
the returned `:operationLocation` reports send status.

For the common single-recipient case, `simple-body` builds that shape from
a flat map:

```clojure
(email/send cfg
  (email/simple-body {:from      "noreply@example.com"
                      :from-name "Example"
                      :to        "user@example.com"
                      :subject   "Welcome"
                      :html      "<p>Hi there</p>"}))
```

Required body fields are `:from` (with `:address` and `:name` — Twilio
rejects a nameless sender), `:to`, and `:content` with `:subject`
and `:html` (`:text` is optional — Twilio derives a plain-text part from
the HTML when omitted). Extra Twilio fields (`:schedule`, `:tags`,
per-recipient `:variables`, etc.) pass through unchanged.

## Error handling

```clojure
(try
  (sms/send cfg msg)
  (catch clojure.lang.ExceptionInfo e
    (case (:type (ex-data e))
      :twilio.core/validation (println "Bad input:" (:errors (ex-data e)))
      :twilio.core/http-error (println "Twilio returned"
                                       (:status (ex-data e))
                                       (:body   (ex-data e)))
      (throw e))))
```

## Development

This project uses [mise](https://mise.jdx.dev/) for Java/Clojure tool
management:

```bash
mise install
clojure -M:test       # run kaocha
```

CI runs the same `clojure -M:test` on push and PR via GitHub Actions.
