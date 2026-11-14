# Logic audit and validation — 1.3.0

**English** | [Русский](AUDIT-1.3.0-RU.md)

The audit used the addon source and local AE2 rv6-stable-7 code. Version 1.3.0 renames the prepared 1.2.1 release and adds bilingual documentation. Gameplay results below apply to the functional code before renumbering; the build and regression checks are rerun for 1.3.0.

## Findings

| Area | Finding and decision |
|---|---|
| ChannelHook reflection | Confirmed unsafe publication. Field resolution is synchronized; the connection field is volatile and published only after accessibility is set. |
| Interface recipe state | Ordinary concurrent ticking was not established. A synchronous AE2 event could mark recipes dirty again; the previous code overwrote that mark after posting. Fixed. |
| Pending item growth | A pending processing job already blocks the next job. Stock-refill leftovers were incorrectly mixed with ingredients destined for a neighboring machine. They now have separate saved recovery storage; further stock refills pause until recovery succeeds. |
| isBusy and canFit | Refusing new work while delivery remains pending is required backpressure. canFit is reached only with an empty processing queue, so it does not need to reserve that queue again. |
| Interface capability | Exposing the real stock buffer on every side is intentional. Configuration samples are ghost items and must not be accessible as real pipe inventory. |
| Cable-side arrays | Local arrays are newly allocated and zero-initialized by Java on each call. The proposed stale-array issue was not confirmed. |
| Vertex color offset | Added alignment, stride and array-length checks. Unknown layouts are left unchanged rather than recolored at an invalid offset. |
| Cell NBT overflow | Invalid or overflowing entries are retained separately as expertInvalidItems. The cell tooltip reports the damage. Such cells cannot be consumed by an upgrade recipe. Ambiguous corrupted data is not automatically reconstructed. |
| Simulated extraction | The proposed mutation was not confirmed: mutation was already inside MODULATE. A full-extraction simulation regression test was added. |
| Nested containers | A foreign container serializing a cell does not by itself cause recursive addon calls. The addon does not enumerate arbitrary foreign inventory NBT; direct insertion of cells is rejected. No speculative foreign-NBT parser was added. |
| Unused chamber matcher | Removed. Recipe matching remains shared with JEI. |
| Fractional EU→RF conversion | Sub-RF energy remains in the EU buffer and NBT. Small-conversion tests confirm a bounded remainder rather than energy loss. |
| IC2 drawEnergy | Added a direction check to prevent withdrawal in input mode. Clamping alone did not establish the claimed energy-creation mechanism. |
| Strict ASM layout check | Retained deliberately for the supported AE2 version. Relaxing it could produce partially widened counters. Mismatches already log the class and cause. Other AE2 versions need separate validation. |
| BusModels initialization | Moved into JVM static initialization, preventing visibility of partially populated maps. |
| Negative slot IDs | -999 and -1 are valid vanilla protocol values and remain supported. Added bounds for other IDs, drag reset on tab changes, hidden-slot protection and transferStackInSlot bounds. |
| Missing gas cell registration | Missing registration now produces an explicit diagnostic exception instead of silently returning an empty stack. |

An additional converter fix prevents rounding up RF consumption when less than one RF's worth of EU buffer space remains. The interface sends its queued-item count to the GUI, shown when hovering over the network indicator. No new background threads or network-wide scans were introduced.

## Validation

- 114 regression checks, including 16,000 parallel first-resolution reflection calls from eight threads.
- 758 baseline gameplay checks: live 32/64/128/256-channel networks, all three storage channels, interface stocking, crafting patterns and the chamber.
- 657 feature checks: 648 server checks for attachments, nine bus filters and collision handling, plus nine JEI checks.
- 8192 real model calls from four threads, without failures or changes to the original cached quads.
- 104 keys per language, matching formatting parameters, and no Russian strings in the English file.

The test profile used its own world; user worlds were not modified. The original source backup is backups/before-1.2.1-audit.zip.

These are bounded automated checks, not a guarantee of zero bugs in every modpack. Long-running multiplayer servers, arbitrary third-party containers and malicious inventory implementations were not fully simulated. Every screen was not manually reviewed in both languages. Historical reports retain their original version numbers.
