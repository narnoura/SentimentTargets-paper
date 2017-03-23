General


This corpus contains entity-level annotations for sentiment in Arabic text. The data is selected from Aljazeera newspaper online comments/posts. The data is written in mostly MSA (modern standard arabic) and corresponds to the online discussion genre.


For more details, please refer to this paper:


Noura Farra, Kathleen McKeown, and Nizar Habash. 2015. Annotating Targets of Opinions in Arabic Using Crowdsourcing. In Proceedings of the ACL-2015 Workshop on Arabic Natural Language processing (ANLP 2015).




Format


The original data comes from four files corresponding to the successive batches annotated on Amazon Mechanical Turk: one batch from the politics domain, one from the sports domain, and two from general culture (language, science, technology and society).
This data has been combined and divided into train, dev and test sets. 

The files are in xml format. For each comment, a comment id and a list of targets are specified with their polarities. Targets which are marked as 'undetermined' should be considered of ambiguous polarity. The indices specify the position of the word in the sentence where the target occurs. If the same target occurs more than once, multiple indices are specified.



Contact


For questions or issues, please contact:


Noura Farra, Columbia University
noura@cs.columbia.edu
