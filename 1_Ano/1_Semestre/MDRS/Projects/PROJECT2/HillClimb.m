function [sol, capLink, Loads, worstLoad, energy, sleepIdx, upIdx, bestTimeFound] = ...
    HillClimb(nNodes, Links, T, sP, nSP, sol, capLink, L, nc, tStart)

    nFlows = size(T,1);
    nLinks = size(Links,1);

    % estado atual
    [Loads, linkE, worstLoad, sleepIdx, upIdx, feas] = ...
        calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, sol, L, capLink);
    nodeE = calculateNodeEnergy(T, sP, nNodes, nc, sol);
    energy = linkE + nodeE;

    bestTimeFound = toc(tStart);

    improved = true;
    while improved
        improved = false;

        bestSol = sol;
        bestCap = capLink;
        bestEnergy = energy;
        bestLoads = Loads;
        bestWorst = worstLoad;
        bestSleep = sleepIdx;
        bestUp = upIdx;

        % (A) vizinhos: mudar path de um flow
        for f = 1:nFlows
            for p = 1:nSP(f)
                if p == sol(f), continue; end
                auxSol = sol; auxSol(f) = p;

                [auxLoads, auxLinkE, auxWorst, auxSleep, auxUp, auxFeas] = ...
                    calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, auxSol, L, bestCap);
                auxNodeE = calculateNodeEnergy(T, sP, nNodes, nc, auxSol);
                auxE = auxLinkE + auxNodeE;

                if auxFeas && auxE < bestEnergy
                    bestEnergy = auxE;
                    bestSol = auxSol;
                    bestLoads = auxLoads;
                    bestWorst = auxWorst;
                    bestSleep = auxSleep;
                    bestUp = auxUp;
                end
            end
        end

        % (B) vizinhos: flip capacidade de um link
        for i = 1:nLinks
            auxCap = bestCap;
            if auxCap(i) == 50
                auxCap(i) = 100;
            else
                auxCap(i) = 50;
            end

            [auxLoads, auxLinkE, auxWorst, auxSleep, auxUp, auxFeas] = ...
                calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, bestSol, L, auxCap);
            auxNodeE = calculateNodeEnergy(T, sP, nNodes, nc, bestSol);
            auxE = auxLinkE + auxNodeE;

            if auxFeas && auxE < bestEnergy
                bestEnergy = auxE;
                bestCap = auxCap;
                bestLoads = auxLoads;
                bestWorst = auxWorst;
                bestSleep = auxSleep;
                bestUp = auxUp;
            end
        end

        % aceitar melhoria
        if bestEnergy < energy
            sol = bestSol;
            capLink = bestCap;
            Loads = bestLoads;
            worstLoad = bestWorst;
            energy = bestEnergy;
            sleepIdx = bestSleep;
            upIdx = bestUp;

            improved = true;
            bestTimeFound = toc(tStart);
        end
    end
end
