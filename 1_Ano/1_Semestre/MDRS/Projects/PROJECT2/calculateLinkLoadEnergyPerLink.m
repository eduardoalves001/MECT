function [Loads, linkEnergy, worstLoad, sleepIdx, upIdx, feasible] = ...
    calculateLinkLoadEnergyPerLink(nNodes, Links, T, sP, sol, L, capLink)

    nFlows = size(T,1);
    nLinks = size(Links,1);
    aux = zeros(nNodes);

    % acumular loads direcionais
    for f = 1:nFlows
        path = sP{f}{sol(f)};
        for j = 2:length(path)
            a = path(j-1); b = path(j);
            aux(a,b) = aux(a,b) + T(f,3);
            aux(b,a) = aux(b,a) + T(f,4);
        end
    end

    Loads = [Links zeros(nLinks,2)];
    linkEnergy = 0;
    sleepIdx = [];
    upIdx = [];
    feasible = true;
    worstLoad = 0;

    for i = 1:nLinks
        a = Loads(i,1); b = Loads(i,2);
        Loads(i,3) = aux(a,b);
        Loads(i,4) = aux(b,a);

        cap = capLink(i);               % 50 ou 100 deste link
        if cap == 100, upIdx(end+1) = i; end

        thisMax = max(Loads(i,3:4));
        worstLoad = max(worstLoad, thisMax);

        if thisMax == 0
            % sleeping
            linkEnergy = linkEnergy + 2;
            sleepIdx(end+1) = i;
        else
            len = L(a,b);
            if cap == 50
                linkEnergy = linkEnergy + (6 + 0.2*len);
            elseif cap == 100
                linkEnergy = linkEnergy + (8 + 0.3*len);
            else
                error('capLink(%d) não é 50 nem 100', i);
            end

            if thisMax > cap
                feasible = false;
            end
        end
    end

    if ~feasible
        linkEnergy = inf;
    end
end
