function sleepingLinks = buildSleepingLinksString(Loads)
    sleepingLinks = '';
    for i = 1:size(Loads, 1)
        if max(Loads(i,3:4)) == 0
            sleepingLinks = append(sleepingLinks, ' {', num2str(Loads(i,1)), ', ', num2str(Loads(i,2)), '}');
        end
    end
end
