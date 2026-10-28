# Love/Sand alpha.55

Heart Step and Sand Drift now use the WASD direction sent with the move press. Sideways/backward dodges follow camera yaw; diagonal movement is normalized. With no movement keys, they default forward. Distances, collision, cooldowns and charge costs remain.

Automatic interruption cleanup now cancels outgoing powers without deleting an incoming petrification or coffin. Active Love/Sand owners can be restrained for the same limited time as other players. Actual disconnect/death still clears incoming and outgoing state. Dark suppression and water stop only the player's outgoing new-fruit powers.

Regression checks register real queried player fixtures, petrify an active Sand owner, coffin an active Love owner, invoke their automatic player updates, check eventual expiry, and exercise directional press/release movement. All earlier regression and client smoke checks remain.
