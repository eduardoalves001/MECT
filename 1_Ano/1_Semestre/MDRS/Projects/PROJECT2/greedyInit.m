function [sol, capLink, Loads, bestEnergy, bestWorstLoad, sleepIdx, upIdx] = ...
    greedyInit(nNodes, Links, T, sP, nSP, L, nc)

    nFlows = size(T,1);
    nLinks = size(Links,1);

    % 1) tudo a 50
    capLink = 50*ones(nLinks,1);

    % 2) solução aleatória inicial de paths (respeitando nSP)
    sol = zeros(1,nFlows);
    for f = 1:nFlows
        sol(f) = randi(nSP(f));
    end

    % 3) avalia
    [Loads, linkE, worstL, sleepIdx, upIdx, feas] = ...
        calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, sol, L, capLink);
    nodeE = calculateNodeEnergy(T, sP, nNodes, nc, sol);

    bestEnergy = linkE + nodeE;
    bestWorstLoad = worstL;

    % 4) se inviável, tenta upgrade só nos links que violam (rápido e eficaz)
    if ~feas
        for i = 1:nLinks
            if max(Loads(i,3:4)) > capLink(i)
                capLink(i) = 100;
            end
        end

        [Loads, linkE, worstL, sleepIdx, upIdx, feas] = ...
            calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, sol, L, capLink);
        nodeE = calculateNodeEnergy(T, sP, nNodes, nc, sol);

        bestEnergy = linkE + nodeE;
        bestWorstLoad = worstL;
    end
end
