function worstLoad = evaluateWorstLinkLoad(sol, paths, Tu, L, nNodes)
% Computes worst link load for a routing solution
% Links are bidirectional - each direction has independent 50 Gbps capacity

linkLoad = zeros(nNodes);

for f = 1:length(sol)
    path = paths{f}{sol(f)};
    up = Tu(f,3);    % upstream traffic (s -> d)
    down = Tu(f,4);  % downstream traffic (d -> s)

    % Add upstream traffic (s -> d direction)
    for i = 1:length(path)-1
        a = path(i);
        b = path(i+1);
        linkLoad(a,b) = linkLoad(a,b) + up;
    end

    % Add downstream traffic (d -> s direction, reverse path)
    for i = 1:length(path)-1
        a = path(i);
        b = path(i+1);
        linkLoad(b,a) = linkLoad(b,a) + down;
    end
end

% Extract worst load considering both directions of each link
loads = [];
for i = 1:nNodes
    for j = i+1:nNodes
        if L(i,j) < inf
            % Take maximum load between both directions
            maxLoad = max(linkLoad(i,j), linkLoad(j,i));
            loads = [loads maxLoad];
        end
    end
end

worstLoad = max(loads); % in Gbps
end
