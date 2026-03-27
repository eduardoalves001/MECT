function [totalEnergy, sleepingLinks] = computeNetworkEnergy(linkLoads, L)

nNodes = size(L,1);
totalEnergy = 0;
sleepingLinks = [];

%% -------- Routers --------
for n = 1:nNodes
    % Router load = outgoing traffic only (avoid double-counting)
    traffic = sum(linkLoads(n,:));
    t = traffic / 500; % router capacity
    En = 10 + 90 * t^2;
    totalEnergy = totalEnergy + En;
end

%% -------- Links ----------
TOLERANCE = 1e-9; % Threshold for considering a link as having no traffic

for i = 1:nNodes
    for j = i+1:nNodes
        if isfinite(L(i,j))  % Link exists (L < Inf for actual physical links)
            % Link is active if there's traffic in either direction (above tolerance)
            if linkLoads(i,j) > TOLERANCE || linkLoads(j,i) > TOLERANCE
                % active link (50 Gbps)
                El = 6 + 0.2 * L(i,j);
            else
                % sleeping link
                El = 2;
                sleepingLinks = [sleepingLinks; i j];
            end
            totalEnergy = totalEnergy + El;
        end
    end
end
end
