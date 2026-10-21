# User Management - flow the automation follows (21 Oct 2026)

Accounts: Maker = automaker, Checker = autochecker (also the branch Checker). Only TEST AUTO data is created or changed.
Order: Roles Positive first, then Users Positive (Add User uses the role approved in Roles).

## Roles Positive (8_01_RolesPositive.feature)
1. Login (config user) -> switch to automaker (Maker).
2. Side nav: User Management > Roles. Check Role Management page + **Add Role** button.
3. Add Role -> unique `TEST AUTO ROLE xxxx`, code, description -> Save. Proof it was queued: row in MY REQUESTS = PENDING (or a clear submitted toast). No toast at all is logged as a BUG.
4. Switch to autochecker -> Roles -> Role Approvals (status Pending) -> open the eye (details: role, maker, type Create, status Pending) -> **Approve** only our row.
5. Details of the approved request are verified (Approved queue).
6. Permission rule check (read only, nothing saved): ticking a child module ticks its parent. Dialog title must name our role, otherwise it closes without a change.
7. **Enable a module permission**: Maker opens Configure Permissions of the TEST AUTO role, ticks the `Dashboard` module checkbox, Save Permissions -> request goes to the Checker.
8. autochecker approves that permission request.
9. Maker re-opens the permissions and confirms `Dashboard` is really ticked (saved).
10. Back to Maker.

## Users Positive (7_01_UsersPositive.feature)
1. Users page as automaker, check **Add User** button.
2. Add User: first/last name, designation, email, mobile, login `testautoNNNNNNNN`, SOL, the approved TEST AUTO role -> Save. Proof: PENDING row in MY REQUESTS.
3. autochecker: User Approvals -> eye -> Approve our row. Then the processed request is checked in the Approved queue.
4. Log in as the new user (default password) to prove access.
5. Maker: Edit designation -> Update -> PENDING in MY REQUESTS -> autochecker approves.
6. Maker: second Edit -> autochecker **Reverts** (the Revert dialog needs a reason; the automation types one) -> checked in the Reverted queue.
7. Lock -> branch checker approves. Unlock -> branch checker approves. Dormant (untick Active) -> branch checker approves.
8. Exit flag / dormant login blocked check. Back to Maker.

## Rules added today
- Every Maker/Checker switch no longer waits 20 s for a pop-up that is not there (it leaves as soon as the app is open).
- Approve and Revert: max 2 tries; after the 2nd failure a screenshot is taken and the step is logged as a BUG, then the run goes on (no endless loop). The Continue pop-up also allows max 2 tries.
- A role action is only done on the TEST AUTO role's own card/row (earlier the permission dialog opened for "Branch Checker").
- Closed browser window stops the step at once with a clear message.
- A small toast (bottom-left, ~4 s) shows "Step n of N", the step being checked, and the next steps still to come. Switch off: `progressToast=false`; length: `progressToastSeconds=4` in config.properties.
- Logs: every login switch prints `Signed in as X (header shows ...) - switch took N ms`; queued checks print `inMyRequestsPending=true/false`.
