function [totalEnergy, feasible, linkLoads, linkCapacities] = evaluateEnergyTask3(solution, paths, Tu, Ta, L, initialLinkCapacities, anycastNodes)
% Evaluate energy consumption for Task 3 with dynamic link capacity upgrades
%
% Key differences from Task 2:
% - Links can be 50 Gbps or 100 Gbps
% - If link load > 50 Gbps, upgrade to 100 Gbps
% - If link load > 100 Gbps, solution is infeasible
% - Upgraded links consume more energy when active

nNodes = size(L,1);
linkCapacities = initialLinkCapacities;

% -------- Compute link loads --------
linkLoads = computeLinkLoads(solution, paths, Tu, nNodes) + computeLinkLoadsAnycast(Ta, anycastNodes, L, nNodes);


% -------- Dynamic capacity upgrade decision --------
% If a link has load > 50 Gbps, upgrade it to 100 Gbps
% If a link has load > 100 Gbps, reject the solution
for i = 1:nNodes
    for j = i+1:nNodes
        if L(i,j) < inf
            load_ij = linkLoads(i,j);
            load_ji = linkLoads(j,i);
            maxLoad = max(load_ij, load_ji);

            if maxLoad > 100
                % Infeasible: exceeds maximum capacity in either direction
                totalEnergy = inf;
                feasible = false;
                return;
            elseif maxLoad > 50
                % Upgrade to 100 Gbps
                linkCapacities(i,j) = 100;
                linkCapacities(j,i) = 100;
            else
                % Keep at 50 Gbps
                linkCapacities(i,j) = 50;
                linkCapacities(j,i) = 50;
            end
        end
    end
end

% Solution is feasible
feasible = true;

% -------- Compute total energy with upgraded link model --------
totalEnergy = 0;

% Router energy (unchanged from Task 2)
for n = 1:nNodes
    traffic = sum(linkLoads(n,:));
    t = traffic / 500; % router capacity
    En = 10 + 90 * t^2;
    totalEnergy = totalEnergy + En;
end

% Link energy (modified for capacity upgrades)
for i = 1:nNodes
    for j = i+1:nNodes
        if L(i,j) < inf
            if linkLoads(i,j) > 0 || linkLoads(j,i) > 0
                % Active link
                if linkCapacities(i,j) == 100
                    % Upgraded link: higher energy consumption
                    % Energy = 6 + 0.2*distance for 50 Gbps
                    % For 100 Gbps, double the variable component
                    El = 8 + 0.3 * L(i,j);
                else
                    % Standard 50 Gbps link
                    El = 6 + 0.2 * L(i,j);
                end
            else
                % Sleeping link (same for both capacities)
                El = 2;
            end
            totalEnergy = totalEnergy + El;
        end
    end
end

end
