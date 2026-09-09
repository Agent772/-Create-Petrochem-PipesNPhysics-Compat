## Version 1.1.0

### Added
- Compatibility with Pipes n' Physics for the distillation tower. Each exposed face now returns its own fluid capability that routes to the correct tank based on the active distillation mode, preventing pipes on opposite faces from merging into a single network and colliding.
- Vacuum mode now requires a running pump on the connected Pipes n' Physics network to draw air from the tower at full rate. Without sufficient suction, only a small passive trickle is allowed (4 mB per drain call by default). The tower's goggle tooltip reports when suction is missing, too slow, or not keeping up.
- Server config (`petrochem_pnp_compat-server.toml`) with `vacuumPassiveDrainMb` (default 4), `vacuumMinPumpRpm` (default 0), and `debugVacuumLogging` (default false).
