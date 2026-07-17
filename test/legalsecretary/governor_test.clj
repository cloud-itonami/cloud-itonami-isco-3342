(ns legalsecretary.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [legalsecretary.store :as store]
            [legalsecretary.advisor :as advisor]
            [legalsecretary.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-attorney! st {:attorney-id "attorney-1" :name "Kobo Law"})
    (store/register-case! st {:case-id "C-1" :attorney-id "attorney-1"
                              :name "case-042"
                              :max-supply-cost 500})
    st))

(def ^:private req {:attorney-id "attorney-1"})

(defn- log-op []
  {:op :log-document-record :effect :propose :case-id "C-1"
   :document-id "D-1" :document-type :filed-motion
   :confidence 0.9 :stake :low})

(defn- schedule-op [attorney-supplied?]
  {:op :schedule-court-operation :effect :propose :case-id "C-1"
   :operation-date "2026-08-01" :attorney-supplied? attorney-supplied?
   :confidence 0.9 :stake :low})

(defn- concern-op []
  {:op :flag-confidentiality-concern :effect :propose :case-id "C-1"
   :concern-detail "possible attorney-client privilege exposure: opposing counsel copied on an internal memo"
   :confidence 0.9 :stake :low})

(defn- supply-op [cost]
  {:op :coordinate-supply-order :effect :propose :case-id "C-1"
   :cost cost :confidence 0.9 :stake :low})

(deftest ok-on-verified-document-log
  (let [st (fresh-store)
        v (governor/check req {} (log-op) st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-attorney-supplied-schedule
  (let [st (fresh-store)
        v (governor/check req {} (schedule-op true) st)]
    (is (:ok? v))))

(deftest hard-on-deadline-not-attorney-supplied
  (testing "setting a filing deadline that was not actually supplied by the
            attorney is an unsupervised legal-strategy decision, not
            routine secretarial scheduling -- HARD, not merely escalate"
    (let [st (fresh-store)
          v (governor/check req {} (schedule-op false) st)]
      (is (:hard? v))
      (is (some #(= :deadline-not-attorney-supplied (:rule %)) (:violations v))))))

(deftest ok-within-supply-cost-threshold
  (let [st (fresh-store)
        v (governor/check req {} (supply-op 200) st)]
    (is (:ok? v))
    (is (not (:escalate? v)))))

(deftest ok-at-exact-supply-cost-threshold-boundary
  (testing "the supply-cost ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (supply-op 500) st)]
      (is (:ok? v)))))

(deftest escalates-over-supply-cost-threshold
  (testing "office/case-material procurement above the registered ceiling
            requires human sign-off, but is not a permanent hard block"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (supply-op 5000) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest hard-on-unknown-op
  (testing "an op outside the closed allowlist -- e.g. one that would provide
            legal advice or determine a deadline unilaterally -- is a
            permanent, non-overridable block, never merely an escalation"
    (let [st (fresh-store)
          v (governor/check req {} {:op :provide-legal-advice :effect :propose
                                    :case-id "C-1" :confidence 0.99 :stake :low
                                    :rationale "gave an opinion on the merits"} st)]
      (is (:hard? v))
      (is (some #(= :unknown-op (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-disclosure-language
  (testing "an allowed op reused to smuggle a forbidden finalize-disclosure
            action through free text is still hard-blocked (defense in depth
            behind the closed allowlist)"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op)
                                          :rationale "finalize the privileged-document disclosure to opposing counsel")
                            st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion-violation (:rule %)) (:violations v))))))

(deftest hard-on-scope-exclusion-legal-advice-language
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :rationale "provide legal advice on liability exposure") st)]
    (is (:hard? v))
    (is (some #(= :scope-exclusion-violation (:rule %)) (:violations v)))))

(deftest hard-on-unknown-case
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :case-id "C-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-case (:rule %)) (:violations v)))))

(deftest hard-on-foreign-case
  (let [st (fresh-store)]
    (store/register-attorney! st {:attorney-id "attorney-2" :name "Other"})
    (let [v (governor/check {:attorney-id "attorney-2"} {} (log-op) st)]
      (is (:hard? v))
      (is (some #(= :case-wrong-attorney (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-attorney
  (let [st (fresh-store)
        v (governor/check {:attorney-id "nobody"} {} (log-op) st)]
    (is (:hard? v))
    (is (some #(= :no-attorney (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-confidentiality-concern-even-at-high-confidence
  (testing "flagging a confidentiality concern always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (concern-op) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest auto-commit-ops-never-include-flag-confidentiality-concern
  (testing "a 'flag a concern' op must always escalate and never be in any
            phase's :auto/auto-commit set"
    (is (not (contains? governor/auto-commit-ops :flag-confidentiality-concern)))
    (is (contains? governor/known-ops :flag-confidentiality-concern))))

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "the governor's own scope-exclusion term list must never match
            inside the mock advisor's own DEFAULT rationale/disclaimer text
            for a legitimate proposal (including :flag-confidentiality-
            concern itself, whose whole purpose is to mention
            confidentiality/privilege) -- phrasing exclusion terms as the
            finalization/execution ACTION rather than the bare noun avoids
            this false self-block"
    (let [st (fresh-store)
          mock (advisor/mock-advisor)
          requests [{:attorney-id "attorney-1" :op :log-document-record :stake :low
                     :case-id "C-1" :document-id "D-1" :document-type :filed-motion}
                    {:attorney-id "attorney-1" :op :schedule-court-operation :stake :low
                     :case-id "C-1" :operation-date "2026-08-01" :attorney-supplied? true}
                    {:attorney-id "attorney-1" :op :flag-confidentiality-concern :stake :low
                     :case-id "C-1"
                     :concern-detail "possible attorney-client privilege exposure: opposing counsel copied on an internal memo"}
                    {:attorney-id "attorney-1" :op :coordinate-supply-order :stake :low
                     :case-id "C-1" :cost 200}]]
      (doseq [request requests]
        (let [proposal (advisor/-advise mock st request)
              v (governor/check request {} proposal st)]
          (is (not (:hard? v))
              (str "op " (:op request) " unexpectedly hard-blocked: " (:violations v))))))))
