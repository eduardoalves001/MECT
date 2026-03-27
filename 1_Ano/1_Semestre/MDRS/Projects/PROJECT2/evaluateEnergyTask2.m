function [energy, feasible] = evaluateEnergyTask2(sol, paths, Tu, L, nNodes, anycastLoad)
% Task 2 energy evaluation (wrapper around Task 1 functions)
% anycastLoad is optional - if not provided, defaults to zero

if nargin < 6
    anycastLoad = zeros(nNodes);
end

linkCapacity = 50; % Gbps (fixed in Task 2)

% Compute link loads
linkLoads = computeLinkLoads(sol, paths, Tu, nNodes);

% Add anycast loads
linkLoads = linkLoads + anycastLoad;

% Feasibility check
if max(linkLoads(:)) > linkCapacity
    feasible = false;
    energy = inf;
    return;
end

feasible = true;

% Compute energy using Task 1 function
[energy, ~] = computeNetworkEnergy(linkLoads, L);
end
