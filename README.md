**Create: Petrochem - Pipes n' Physics Compat**

Fixes fluid handling between Create Petrochem's distillation tower and Create Pipes n' Physics. Without this mod, pipes can't properly push fluids into the tower.

*This mod was made with AI.*

**What it does**

The distillation tower exposes a single fluid capability that doesn't play nice with Pipes n' Physics. This mod replaces it with per-side capabilities so each exposed face routes to the correct input tank. Extraction is blocked on input tanks to match Petrochem's intended behavior.

**Vacuum suction**

In VACUUM mode a bare pipe used to drain the tower's air on its own, because Pipes n' Physics equalises gases through the output face without requiring a pump. The tower now only lets a pipe pull more than a small trickle of air when a pump is actually running on the attached network, so keeping a vacuum takes real suction. When the tower can't reach vacuum for want of suction, its goggle tooltip explains why (no pump, pump too slow, or the pump not keeping up).

Any pump anywhere on the connected network counts, regardless of where it sits or which way it faces.

**Config**

Server config (`petrochem_pnp_compat-server.toml`):

- `vacuumPassiveDrainMb` (default 4) - air a pipe may pull per drain call while no pump runs, in mB.
- `vacuumMinPumpRpm` (default 0) - pump RPM on the network needed to unlock full-rate draining. 0 accepts any turning pump.
- `debugVacuumLogging` (default false) - log air level, suction and the measured drain rate once per second per tower, at DEBUG level.

No new blocks. No new items. Just makes the two mods work together.