(ns legalsecretary.governor
  "LegalSecretaryGovernor — the independent safety/confidentiality
  layer named in this repository's README/business-model.md, gating
  every legal-secretarial operation an advisor may propose for a
  case. The governor never dispatches hardware itself and never
  finalizes disclosure of privileged/confidential case information,
  provides legal advice, or sets a filing deadline/legal strategy
  without attorney sign-off. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor. Task twist: legal secretaries handle
  privileged case files, so the closed op allowlist itself is the
  primary confidentiality guardrail (no op in it can finalize
  disclosure, give advice or set strategy/deadlines unilaterally),
  backed by a defense-in-depth scope-exclusion check on proposal text
  for any op that is reused to smuggle a forbidden action through.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. attorney provenance     — the supervising attorney must be
                                REGISTERED before any action.
    2. no-actuation            — proposal :effect must be :propose
                                (the governor never dispatches
                                hardware and never itself finalizes a
                                privileged disclosure; it only gates
                                what the advisor may log/schedule/
                                flag/order).
    3. closed op allowlist     — proposal :op must be one of
                                :log-document-record,
                                :schedule-court-operation,
                                :flag-confidentiality-concern,
                                :coordinate-supply-order. Any other
                                op (e.g. one that would directly
                                finalize disclosure of privileged
                                case information, provide legal
                                advice, or determine a filing
                                deadline/legal strategy on its own
                                authority) is a hard, permanent block
                                -- it is not, and will never become,
                                auto-commit-eligible.
    4. case basis               — a case-scoped proposal must cite a
                                REGISTERED case belonging to this
                                attorney.
    5. attorney-supplied deadline — a :schedule-court-operation
                                proposal must have
                                `:attorney-supplied? true` (setting a
                                filing deadline the attorney did not
                                actually supply is exactly the
                                unsupervised legal-strategy decision
                                this actor must never make).
    6. scope-exclusion action check — the proposal's free text
                                (:rationale / :concern-detail) must
                                not describe FINALIZING a privileged
                                disclosure, providing legal advice, or
                                determining a deadline/strategy
                                without sign-off. Phrased as the
                                finalization/execution ACTION (not
                                bare nouns like \"privilege\" or
                                \"advice\", which appear in the
                                default mock advisor's own legitimate
                                :flag-confidentiality-concern
                                rationale) so a legitimate concern-flag
                                never self-trips this check -- see
                                `legalsecretary.governor-test`'s
                                `default-mock-advisor-proposals-never-
                                self-trip-scope-exclusion`.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls -- these are :high/
  :safety-critical regardless of confidence, and NONE of them are
  ever added to `auto-commit-ops`):
    7. :op :flag-confidentiality-concern always escalates -- a
       confidentiality/privilege concern is surfaced to a human, never
       auto-resolved, regardless of confidence.
    8. supply orders above the case's registered `:max-supply-cost`
       ceiling.
    9. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [legalsecretary.store :as store]))

(def confidence-floor 0.6)

;; The closed proposal allowlist. No op that directly finalizes
;; disclosure of privileged/confidential case information, provides
;; legal advice, or determines a filing deadline/legal strategy
;; without attorney sign-off is ever a member of this set -- adding
;; such an op is a permanent-block design violation, not a policy
;; toggle.
(def known-ops #{:log-document-record
                  :schedule-court-operation
                  :flag-confidentiality-concern
                  :coordinate-supply-order})

;; Ops that always require human sign-off and are therefore NEVER
;; eligible for auto-commit, no matter the confidence or phase.
(def ^:private always-escalate-ops #{:flag-confidentiality-concern})

;; Ops that MAY be auto-committed when `:ok?` -- deliberately excludes
;; :flag-confidentiality-concern (see `always-escalate-ops` above);
;; a "flag a concern" op must never be auto-commit-eligible.
(def auto-commit-ops (into #{} (remove always-escalate-ops known-ops)))

;; Scope-exclusion phrases are written as the finalization/execution
;; ACTION, not the bare noun, precisely so the mock advisor's own
;; default :flag-confidentiality-concern rationale (which legitimately
;; mentions "confidentiality"/"privilege" as the concern being raised)
;; never matches. A legitimate proposal describes NOTICING a risk; a
;; forbidden proposal describes CARRYING OUT the disclosure/advice/
;; strategy decision.
(def ^:private scope-exclusion-phrases
  ["finalize the privileged-document disclosure"
   "finalize disclosure of privileged"
   "disclose the privileged case file"
   "disclose privileged case information"
   "waive the privilege"
   "provide legal advice"
   "give legal advice"
   "determine the filing deadline without attorney"
   "set the legal strategy without attorney"
   "determine legal strategy without attorney"])

(defn- scope-exclusion-violation? [proposal]
  (let [text (str/lower (str (:rationale proposal) " " (:concern-detail proposal)))]
    (boolean (some #(str/includes? text %) scope-exclusion-phrases))))

(def ^:private case-scoped-ops
  "Every known op is case-scoped in this domain -- there is no
  attorney-level operation that skips case provenance."
  known-ops)

(defn- hard-violations [{:keys [request proposal]} attorney-record c]
  (let [{:keys [op attorney-supplied?]} proposal
        known? (contains? known-ops op)
        case-op? (and known? (contains? case-scoped-ops op))]
    (cond-> []
      (nil? attorney-record)
      (conj {:rule :no-attorney :detail "未登録 attorney"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は特権情報開示・法的助言・期限/戦略決定を直接実行しない）"})

      (not known?)
      (conj {:rule :unknown-op
             :detail "closed allowlist に存在しない operation（特権情報開示の確定・法的助言の提供・弁護士承認なしの期限/戦略決定はこの secretarial actor の権限外）"})

      (and case-op? (nil? c))
      (conj {:rule :unknown-case :detail "未登録 case への提案は不可"})

      (and case-op? c (not= (:attorney-id c) (:attorney-id request)))
      (conj {:rule :case-wrong-attorney :detail "case が別 attorney のもの"})

      (and known? (= :schedule-court-operation op) (not attorney-supplied?))
      (conj {:rule :deadline-not-attorney-supplied
             :detail "弁護士承認由来でない期限設定は、弁護士のサインオフなしの法的期限決定であって秘書業務ではない"})

      (scope-exclusion-violation? proposal)
      (conj {:rule :scope-exclusion-violation
             :detail "特権情報開示の確定・法的助言の提供・弁護士承認なしの期限/戦略決定を実行する提案は permanent block"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `legalsecretary.store/Store`. Pure -- never
  mutates the store, never finalizes a privileged disclosure, never
  provides legal advice, never sets a deadline/strategy the attorney
  did not supply."
  [request context proposal store]
  (let [attorney-record (store/attorney store (:attorney-id request))
        c (some->> (:case-id proposal) (store/case-record store))
        hard (hard-violations {:request request :proposal proposal}
                              attorney-record c)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        over-cost? (and (not hard?)
                        (= :coordinate-supply-order (:op proposal))
                        c
                        (number? (:cost proposal))
                        (number? (:max-supply-cost c))
                        (> (:cost proposal) (:max-supply-cost c)))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-cost?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-cost?))}))
